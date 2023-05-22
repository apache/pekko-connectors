/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ClassicActorSystemProvider
import pekko.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import pekko.stream.connectors.googlecloud.bigquery.storage.impl.SDKClientSource
import pekko.stream.scaladsl.Source
import pekko.stream.{ Attributes, Materializer }
import pekko.util.ByteString
import com.google.cloud.bigquery.storage.v1.DataFormat
import com.google.cloud.bigquery.storage.v1.storage.{ BigQueryReadClient, CreateReadSessionRequest, ReadRowsResponse }
import com.google.cloud.bigquery.storage.v1.stream.ReadSession.TableReadOptions
import com.google.cloud.bigquery.storage.v1.stream.ReadSession

import com.google.cloud.bigquery.storage.v1.stream.{ DataFormat => StreamDataFormat }

import scala.concurrent.{ ExecutionContextExecutor, Future }

/**
 * Google BigQuery Storage Api Pekko Stream operator factory.
 */
object BigQueryStorage {

  private val RequestParamsHeader = "x-goog-request-params"

  def create(
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0)
      : Source[(ReadSession.Schema, Seq[Source[ReadRowsResponse.Rows, NotUsed]]), Future[NotUsed]] =
    Source.fromMaterializer { (mat, attr) =>
      val client = reader(mat.system, attr).client
      readSession(client, projectId, datasetId, tableId, dataFormat, readOptions, maxNumStreams)
        .map { session =>
          (session.schema, SDKClientSource.read(client, session))
        }
    }

  def createMergedStreams[A](
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0)(implicit um: FromByteStringUnmarshaller[A]): Source[A, Future[NotUsed]] = {
    Source.fromMaterializer { (mat, attr) =>
      {
        implicit val materializer: Materializer = mat
        implicit val executionContext: ExecutionContextExecutor = materializer.executionContext
        val client = reader(mat.system, attr).client
        readSession(client, projectId, datasetId, tableId, dataFormat, readOptions, maxNumStreams)
          .map { session =>
            SDKClientSource.read(client, session).map { source =>
              source
                .mapAsync(1)(resp => {
                  val bytes =
                    if (resp.isArrowRecordBatch)
                      resp.arrowRecordBatch.get.serializedRecordBatch
                    else
                      resp.avroRows.get.serializedBinaryRows
                  um(ByteString(bytes.toByteArray))
                })
            }
          }
          .map(a => a.reduceOption((a, b) => a.merge(b)))
          .filter(a => a.isDefined)
          .flatMapConcat(a => a.get)
      }
    }
  }

  private[scaladsl] def readSession(client: BigQueryReadClient,
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0) =
    Source
      .future {
        val table = s"projects/$projectId/datasets/$datasetId/tables/$tableId"
        client
          .createReadSession()
          .addHeader(RequestParamsHeader, s"read_session.table=$table")
          .invoke(
            CreateReadSessionRequest(
              parent = s"projects/$projectId",
              Some(
                ReadSession(dataFormat = StreamDataFormat.fromValue(dataFormat.getNumber),
                  table = table,
                  readOptions = readOptions)),
              maxNumStreams))
      }

  private[scaladsl] def reader(system: ClassicActorSystemProvider, attr: Attributes) =
    attr
      .get[BigQueryStorageAttributes.BigQueryStorageReader]
      .map(_.client)
      .getOrElse(GrpcBigQueryStorageReaderExt()(system).reader)
}
