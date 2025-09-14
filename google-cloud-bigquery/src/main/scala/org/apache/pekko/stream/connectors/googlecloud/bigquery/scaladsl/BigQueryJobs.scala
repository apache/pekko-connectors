/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ClassicActorSystemProvider
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.http.scaladsl.marshalling.{ Marshal, ToEntityMarshaller }
import pekko.http.scaladsl.model.ContentTypes.`application/octet-stream`
import pekko.http.scaladsl.model.HttpMethods.POST
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.model.{ HttpEntity, HttpRequest, RequestEntity }
import pekko.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import pekko.stream.FlowShape
import pekko.stream.connectors.google.implicits._
import pekko.stream.connectors.google.scaladsl.`X-Upload-Content-Type`
import pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import pekko.stream.connectors.googlecloud.bigquery._
import pekko.stream.connectors.googlecloud.bigquery.model.CreateDisposition.CreateNever
import pekko.stream.connectors.googlecloud.bigquery.model.SourceFormat.NewlineDelimitedJsonFormat
import pekko.stream.connectors.googlecloud.bigquery.model.{
  Job,
  JobCancelResponse,
  JobConfiguration,
  JobConfigurationLoad
}
import pekko.stream.connectors.googlecloud.bigquery.model.TableReference
import pekko.stream.connectors.googlecloud.bigquery.model.WriteDisposition.WriteAppend
import pekko.stream.scaladsl.{ Flow, GraphDSL, Keep, Sink }
import pekko.util.ByteString

import scala.annotation.nowarn
import scala.concurrent.{ ExecutionContext, Future }

private[scaladsl] trait BigQueryJobs { this: BigQueryRest =>

  /**
   * Returns information about a specific job.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/get BigQuery reference]]
   *
   * @param jobId job ID of the requested job
   * @param location the geographic location of the job. Required except for US and EU
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Job]]
   */
  def job(jobId: String, location: Option[String] = None)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Job] = {
    import BigQueryException._
    import SprayJsonSupport._
    val uri = BigQueryEndpoints.job(settings.projectId, jobId)
    val query = ("location" -> location) ?+: Query.Empty
    singleRequest[Job](HttpRequest(uri = uri.withQuery(query)))
  }

  /**
   * Requests that a job be cancelled.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/cancel BigQuery reference]]
   *
   * @param jobId job ID of the job to cancel
   * @param location the geographic location of the job. Required except for US and EU
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.JobCancelResponse]]
   */
  def cancelJob(
      jobId: String,
      location: Option[String] = None)(
      implicit system: ClassicActorSystemProvider, settings: GoogleSettings): Future[JobCancelResponse] = {
    import BigQueryException._
    import SprayJsonSupport._
    val uri = BigQueryEndpoints.jobCancel(settings.projectId, jobId)
    val query = ("location" -> location) ?+: Query.Empty
    singleRequest[JobCancelResponse](HttpRequest(POST, uri.withQuery(query)))
  }

  /**
   * Loads data into BigQuery via a series of asynchronous load jobs created at the rate [[pekko.stream.connectors.googlecloud.bigquery.BigQuerySettings.loadJobPerTableQuota]].
   * @note WARNING: Pending the resolution of [[https://issuetracker.google.com/176002651 BigQuery issue 176002651]] this method may not work as expected.
   *       As a workaround, you can use the config setting `pekko.http.parsing.conflicting-content-type-header-processing-mode = first` with Pekko HTTP.
   *
   * @param datasetId dataset ID of the table to insert into
   * @param tableId table ID of the table to insert into
   * @tparam In the data model for each record
   * @return a [[pekko.stream.scaladsl.Flow]] that uploads each [[In]] and emits a [[pekko.stream.connectors.googlecloud.bigquery.model.Job]] for every upload job created
   */
  def insertAllAsync[In: ToEntityMarshaller](datasetId: String, tableId: String): Flow[In, Job, NotUsed] =
    insertAllAsync(datasetId, tableId, None)

  /**
   * Loads data into BigQuery via a series of asynchronous load jobs created at the rate [[pekko.stream.connectors.googlecloud.bigquery.BigQuerySettings.loadJobPerTableQuota]].
   * @note WARNING: Pending the resolution of [[https://issuetracker.google.com/176002651 BigQuery issue 176002651]] this method may not work as expected.
   *       As a workaround, you can use the config setting `pekko.http.parsing.conflicting-content-type-header-processing-mode = first` with Pekko HTTP.
   *
   * @param datasetId dataset ID of the table to insert into
   * @param tableId table ID of the table to insert into
   * @param labels the labels associated with this job
   * @tparam In the data model for each record
   * @return a [[pekko.stream.scaladsl.Flow]] that uploads each [[In]] and emits a [[pekko.stream.connectors.googlecloud.bigquery.model.Job]] for every upload job created
   */
  def insertAllAsync[In: ToEntityMarshaller](datasetId: String,
      tableId: String,
      labels: Option[Map[String, String]]): Flow[In, Job, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        import SprayJsonSupport._
        import mat.executionContext
        implicit val settings: GoogleSettings = GoogleAttributes.resolveSettings(mat, attr)
        val BigQuerySettings(loadJobPerTableQuota) = BigQueryAttributes.resolveSettings(mat, attr)

        val job = Job(
          Some(
            JobConfiguration(
              Some(
                JobConfigurationLoad(
                  None,
                  Some(TableReference(Some(settings.projectId), datasetId, Some(tableId))),
                  Some(CreateNever),
                  Some(WriteAppend),
                  Some(NewlineDelimitedJsonFormat))),
              labels)),
          None,
          None)

        val jobFlow = {
          val newline = ByteString("\n")
          val sink = Flow[In]
            .takeWithin(loadJobPerTableQuota)
            .mapAsync(1)(Marshal(_).to[HttpEntity])
            .flatMapConcat(_.dataBytes)
            .intersperse(newline)
            .toMat(createLoadJob(job))(Keep.right)
          Flow.fromGraph(GraphDSL.createGraph(sink) { implicit b => sink =>
            import GraphDSL.Implicits._
            FlowShape(sink.in, b.materializedValue.mapAsync(1)(identity).outlet)
          })
        }

        Flow[In]
          .groupBy(1, _ => (), allowClosedSubstreamRecreation = true)
          .via(jobFlow)
          .concatSubstreams
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Starts a new asynchronous upload job.
   * @note WARNING: Pending the resolution of [[https://issuetracker.google.com/176002651 BigQuery issue 176002651]] this method may not work as expected.
   *       As a workaround, you can use the config setting `pekko.http.parsing.conflicting-content-type-header-processing-mode = first` with Pekko HTTP.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/insert BigQuery reference]]
   * @see [[https://cloud.google.com/bigquery/docs/reference/api-uploads BigQuery reference]]
   *
   * @param job the job to start
   * @tparam Job the data model for a job
   * @return a [[pekko.stream.scaladsl.Sink]] that uploads bytes and materializes a [[scala.concurrent.Future]] containing the [[Job]] when completed
   */
  def createLoadJob[@nowarn("msg=shadows") Job: ToEntityMarshaller: FromEntityUnmarshaller](
      job: Job): Sink[ByteString, Future[Job]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        import BigQueryException._
        implicit val settings: GoogleSettings = GoogleAttributes.resolveSettings(mat, attr)
        implicit val ec: ExecutionContext = ExecutionContext.parasitic
        val uri = BigQueryMediaEndpoints.jobs(settings.projectId).withQuery(Query("uploadType" -> "resumable"))
        Sink
          .lazyFutureSink { () =>
            Marshal(job)
              .to[RequestEntity]
              .map { entity =>
                val request = HttpRequest(POST, uri, List(`X-Upload-Content-Type`(`application/octet-stream`)), entity)
                resumableUpload[Job](request)
              }(ExecutionContext.parasitic)
          }
          .mapMaterializedValue(_.flatten)
      }
      .mapMaterializedValue(_.flatten)

}
