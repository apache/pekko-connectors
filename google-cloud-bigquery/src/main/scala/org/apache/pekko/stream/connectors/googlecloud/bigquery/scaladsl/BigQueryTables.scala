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
import pekko.Done
import pekko.actor.ClassicActorSystemProvider
import pekko.dispatch.ExecutionContexts
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.http.scaladsl.marshalling.Marshal
import pekko.http.scaladsl.model.HttpMethods.{ DELETE, POST }
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.model.{ HttpRequest, RequestEntity }
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.google.implicits._
import pekko.stream.connectors.googlecloud.bigquery.model.{ Table, TableListResponse, TableReference }
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.schema.TableSchemaWriter
import pekko.stream.connectors.googlecloud.bigquery.{ BigQueryEndpoints, BigQueryException }
import pekko.stream.scaladsl.{ Keep, Sink, Source }

import scala.concurrent.Future

private[scaladsl] trait BigQueryTables { this: BigQueryRest =>

  /**
   * Lists all tables in the specified dataset.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/list BigQuery reference]]
   *
   * @param datasetId dataset ID of the tables to list
   * @param maxResults the maximum number of results to return in a single response page
   * @return a [[pekko.stream.scaladsl.Source]] that emits each [[pekko.stream.connectors.googlecloud.bigquery.model.Table]] in the dataset and materializes a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.TableListResponse]]
   */
  def tables(datasetId: String, maxResults: Option[Int] = None): Source[Table, Future[TableListResponse]] =
    source { settings =>
      import BigQueryException._
      import SprayJsonSupport._
      val uri = BigQueryEndpoints.tables(settings.projectId, datasetId)
      val query = ("maxResults" -> maxResults) ?+: Query.Empty
      paginatedRequest[TableListResponse](HttpRequest(uri = uri.withQuery(query)))
    }.wireTapMat(Sink.head)(Keep.right).mapConcat(_.tables.fold(List.empty[Table])(_.toList))

  /**
   * Gets the specified table resource. This method does not return the data in the table, it only returns the table resource, which describes the structure of this table.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/get BigQuery reference]]
   *
   * @param datasetId dataset ID of the requested table
   * @param tableId table ID of the requested table
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Table]]
   */
  def table(datasetId: String, tableId: String)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Table] = {
    import BigQueryException._
    import SprayJsonSupport._
    val uri = BigQueryEndpoints.table(settings.projectId, datasetId, tableId)
    singleRequest[Table](HttpRequest(uri = uri))
  }

  /**
   * Creates a new, empty table in the dataset.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/insert BigQuery reference]]
   *
   * @param datasetId dataset ID of the new table
   * @param tableId table ID of the new table
   * @tparam T the data model for the records of this table
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Table]]
   */
  def createTable[T](datasetId: String, tableId: String)(
      implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings,
      schemaWriter: TableSchemaWriter[T]): Future[Table] = {
    val table = Table(TableReference(None, datasetId, Some(tableId)), None, Some(schemaWriter.write), None, None)
    createTable(table)
  }

  /**
   * Creates a new, empty table in the dataset.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/insert BigQuery reference]]
   *
   * @param table the [[pekko.stream.connectors.googlecloud.bigquery.model.Table]] to create
   * @return a [[scala.concurrent.Future]] containing the [[pekko.stream.connectors.googlecloud.bigquery.model.Table]]
   */
  def createTable(table: Table)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Table] = {
    import BigQueryException._
    import SprayJsonSupport._
    implicit val ec = ExecutionContexts.parasitic
    val projectId = table.tableReference.projectId.getOrElse(settings.projectId)
    val datasetId = table.tableReference.datasetId
    val uri = BigQueryEndpoints.tables(projectId, datasetId)
    Marshal(table).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(POST, uri, entity = entity)
      singleRequest[Table](request)
    }
  }

  /**
   * Deletes the specified table from the dataset. If the table contains data, all the data will be deleted.
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/delete BigQuery reference]]
   *
   * @param datasetId dataset ID of the table to delete
   * @param tableId table ID of the table to delete
   * @return a [[scala.concurrent.Future]] containing [[pekko.Done]]
   */
  def deleteTable(datasetId: String, tableId: String)(implicit system: ClassicActorSystemProvider,
      settings: GoogleSettings): Future[Done] = {
    import BigQueryException._
    val uri = BigQueryEndpoints.table(settings.projectId, datasetId, tableId)
    singleRequest[Done](HttpRequest(DELETE, uri))
  }

}
