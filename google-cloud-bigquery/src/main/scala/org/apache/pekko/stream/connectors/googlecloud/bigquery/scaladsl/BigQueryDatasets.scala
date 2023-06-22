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
import pekko.actor.ClassicActorSystemProvider
import pekko.dispatch.ExecutionContexts
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.http.scaladsl.marshalling.Marshal
import pekko.http.scaladsl.model.HttpMethods.{ DELETE, POST }
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.model.{ HttpRequest, RequestEntity }
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.google.implicits._
import pekko.stream.connectors.googlecloud.bigquery.model.{ Dataset, DatasetListResponse, DatasetReference }
import pekko.stream.connectors.googlecloud.bigquery.{ BigQueryEndpoints, BigQueryException }
import pekko.stream.scaladsl.Source
import pekko.{ Done, NotUsed }

import scala.concurrent.Future

private[scaladsl] trait BigQueryDatasets { this: BigQueryRest =>

  /**
   * Lists all datasets in the specified project to which the user has been granted the READER dataset role.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/list BigQuery reference]]
   *
   * @return a [[pekko.stream.scaladsl.Source]] that emits each [[pekko.stream.connectors.googlecloud.bigquery.model.Dataset]]
   */
  def datasets: Source[Dataset, NotUsed] = datasets()

  /**
   * Lists all datasets in the specified project to which the user has been granted the READER dataset role.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/list BigQuery reference]]
   *
   * @param maxResults the maximum number of results to return in a single response page
   * @param all whether to list all datasets, including hidden ones
   * @param filter a key, value [[scala.collection.immutable.Map]] for filtering the results of the request by label
   * @return a [[pekko.stream.scaladsl.Source]] that emits each [[pekko.stream.connectors.googlecloud.bigquery.model.Dataset]]
   */
  def datasets(maxResults: Option[Int] = None,
      all: Option[Boolean] = None,
      filter: Map[String, String] = Map.empty): Source[Dataset, NotUsed] =
    source { settings =>
      import BigQueryException._
      import SprayJsonSupport._
      val uri = BigQueryEndpoints.datasets(settings.projectId)
      val query = ("maxResults" -> maxResults) ?+:
        ("all" -> all) ?+:
        ("filter" -> (if (filter.isEmpty) None else Some(mkFilterParam(filter)))) ?+:
        Query.Empty
      paginatedRequest[DatasetListResponse](HttpRequest(uri = uri.withQuery(query)))
    }.mapMaterializedValue(_ => NotUsed).mapConcat(_.datasets.fold(List.empty[Dataset])(_.toList))

  /**
   * Returns the specified dataset.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/get BigQuery reference]]
   *
   * @param datasetId dataset ID of the requested dataset
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Dataset]]
   */
  def dataset(datasetId: String)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Dataset] = {
    import BigQueryException._
    import SprayJsonSupport._
    val uri = BigQueryEndpoints.dataset(settings.projectId, datasetId)
    singleRequest[Dataset](HttpRequest(uri = uri))
  }

  /**
   * Creates a new empty dataset.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/insert BigQuery reference]]
   *
   * @param datasetId dataset ID of the new dataset
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Dataset]]
   */
  def createDataset(datasetId: String)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Dataset] = {
    val dataset = Dataset(DatasetReference(Some(datasetId), None), None, None, None)
    createDataset(dataset)
  }

  /**
   * Creates a new empty dataset.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/insert BigQuery reference]]
   *
   * @param dataset the [[pekko.stream.connectors.googlecloud.bigquery.model.Dataset]] to create
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Dataset]]
   */
  def createDataset(dataset: Dataset)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Dataset] = {
    import BigQueryException._
    import SprayJsonSupport._
    implicit val ec = ExecutionContexts.parasitic
    val uri = BigQueryEndpoints.datasets(settings.projectId)
    Marshal(dataset).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(POST, uri, entity = entity)
      singleRequest[Dataset](request)
    }
  }

  /**
   * Deletes the dataset specified by the datasetId value.
   *
   * @param datasetId dataset ID of dataset being deleted
   * @param deleteContents if `true`, delete all the tables in the dataset; if `false` and the dataset contains tables, the request will fail
   * @return a [[scala.concurrent.Future]] containing [[pekko.Done]]
   */
  def deleteDataset(datasetId: String, deleteContents: Boolean = false)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Done] = {
    import BigQueryException._
    val uri = BigQueryEndpoints.dataset(settings.projectId, datasetId)
    val query = Query("deleteContents" -> deleteContents.toString)
    singleRequest[Done](HttpRequest(DELETE, uri.withQuery(query)))
  }

}
