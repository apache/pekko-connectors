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
import pekko.actor.ActorSystem
import pekko.NotUsed
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.http.scaladsl.marshalling.Marshal
import pekko.http.scaladsl.model.HttpMethods.POST
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.model.{ HttpRequest, RequestEntity }
import pekko.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import pekko.stream.RestartSettings
import pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import pekko.stream.connectors.google.implicits._
import pekko.stream.connectors.googlecloud.bigquery.model.JobReference
import pekko.stream.connectors.googlecloud.bigquery.model.{ QueryRequest, QueryResponse }
import pekko.stream.connectors.googlecloud.bigquery.{ BigQueryEndpoints, BigQueryException }
import pekko.stream.scaladsl.{ Keep, RestartSource, Sink, Source }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

private[scaladsl] trait BigQueryQueries { this: BigQueryRest =>

  /**
   * Runs a BigQuery SQL query.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query BigQuery reference]]
   *
   * @param query a query string, following the BigQuery query syntax, of the query to execute
   * @param dryRun if set to `true` BigQuery doesn't run the job and instead returns statistics about the job such as how many bytes would be processed
   * @param useLegacySql specifies whether to use BigQuery's legacy SQL dialect for this query
   * @tparam Out the data model of the query results
   * @return a [[pekko.stream.scaladsl.Source]] that emits an [[Out]] for each row of the result and materializes
   *         a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse]]
   */
  def query[Out](query: String, dryRun: Boolean = false, useLegacySql: Boolean = true)(
      implicit um: FromEntityUnmarshaller[QueryResponse[Out]]): Source[Out, Future[QueryResponse[Out]]] = {
    val request = QueryRequest(query, None, None, None, Some(dryRun), Some(useLegacySql), None)
    this.query(request).mapMaterializedValue(_._2)
  }

  /**
   * Runs a BigQuery SQL query.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query BigQuery reference]]
   *
   * @param query the [[pekko.stream.connectors.googlecloud.bigquery.model.QueryRequest]]
   * @tparam Out the data model of the query results
   * @return a [[pekko.stream.scaladsl.Source]] that emits an [[Out]] for each row of the results and materializes
   *         a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.JobReference]] and
   *         a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse]]
   */
  def query[Out](query: QueryRequest)(
      implicit um: FromEntityUnmarshaller[QueryResponse[Out]])
      : Source[Out, (Future[JobReference], Future[QueryResponse[Out]])] =
    Source
      .fromMaterializer { (mat, attr) =>
        import BigQueryException._
        import SprayJsonSupport._
        implicit val system: ActorSystem = mat.system
        implicit val ec: ExecutionContext = ExecutionContext.parasitic
        implicit val settings: GoogleSettings = GoogleAttributes.resolveSettings(mat, attr)

        Source.lazyFutureSource { () =>
          for {
            entity <- Marshal(query).to[RequestEntity]
            initialRequest = HttpRequest(POST, BigQueryEndpoints.queries(settings.projectId), entity = entity)
            initialQueryResponse <- singleRequest[QueryResponse[Out]](initialRequest)
          } yield {

            val jobReference = initialQueryResponse.jobReference

            val head =
              if (initialQueryResponse.jobComplete)
                Source.single(initialQueryResponse)
              else
                Source.empty

            val tail =
              if (initialQueryResponse.jobComplete & initialQueryResponse.pageToken.isEmpty)
                Source.empty
              else
                jobReference.jobId.map { jobId =>
                  import settings.requestSettings.retrySettings._
                  val pages = queryResultsPages[Out](jobId,
                    None,
                    query.maxResults,
                    query.timeout,
                    initialQueryResponse.jobReference.location,
                    initialQueryResponse.pageToken)
                    .map(Success(_))
                    .recover { case ex => Failure(ex) } // Allows upstream failures to escape the RestartSource
                    .map { queryResponse =>
                      if (queryResponse.toOption.forall(_.jobComplete))
                        queryResponse
                      else
                        throw BigQueryException("Query job not complete.")
                    }
                    .addAttributes(attr)
                  val restartSettings = RestartSettings(minBackoff, maxBackoff, randomFactor)
                  RestartSource.onFailuresWithBackoff(restartSettings)(() => pages).map(_.get)
                }.getOrElse(Source.empty)

            head.concat(tail).mapMaterializedValue(_ => jobReference)
          }

        }

      }
      .mapMaterializedValue(_.flatten)
      .wireTapMat(Sink.head)(Keep.both)
      .mapConcat(_.rows.fold[List[Out]](Nil)(_.toList))

  /**
   * The results of a query job.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/getQueryResults BigQuery reference]]
   *
   * @param jobId job ID of the query job
   * @param startIndex zero-based index of the starting row
   * @param maxResults maximum number of results to read
   * @param timeout specifies the maximum amount of time that the client is willing to wait for the query to complete
   * @param location the geographic location of the job. Required except for US and EU
   * @tparam Out the data model of the query results
   * @return a [[pekko.stream.scaladsl.Source]] that emits an [[Out]] for each row of the results and materializes a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse]]
   */
  def queryResults[Out](
      jobId: String,
      startIndex: Option[Long] = None,
      maxResults: Option[Int] = None,
      timeout: Option[FiniteDuration] = None,
      location: Option[String] = None)(
      implicit um: FromEntityUnmarshaller[QueryResponse[Out]]): Source[Out, Future[QueryResponse[Out]]] =
    queryResultsPages(jobId, startIndex, maxResults, timeout, location, None)
      .wireTapMat(Sink.head)(Keep.right)
      .mapConcat(_.rows.fold[List[Out]](Nil)(_.toList))

  private def queryResultsPages[Out](
      jobId: String,
      startIndex: Option[Long],
      maxResults: Option[Int],
      timeout: Option[FiniteDuration],
      location: Option[String],
      pageToken: Option[String])(
      implicit um: FromEntityUnmarshaller[QueryResponse[Out]]): Source[QueryResponse[Out], NotUsed] =
    source { settings =>
      import BigQueryException._
      val uri = BigQueryEndpoints.query(settings.projectId, jobId)
      val query = ("startIndex" -> startIndex) ?+:
        ("maxResults" -> maxResults) ?+:
        ("timeoutMs" -> timeout.map(_.toMillis)) ?+:
        ("location" -> location) ?+:
        ("pageToken" -> pageToken) ?+:
        Query.Empty
      paginatedRequest[QueryResponse[Out]](HttpRequest(uri = uri.withQuery(query)))
    }.mapMaterializedValue(_ => NotUsed)

}
