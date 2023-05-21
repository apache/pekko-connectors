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
import pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord
import pekko.stream.connectors.googlecloud.bigquery.storage.impl.ArrowSource
import pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.BigQueryStorage.{ readSession, reader }
import pekko.stream.scaladsl.Source
import com.google.cloud.bigquery.storage.v1.arrow.{ ArrowRecordBatch, ArrowSchema }
import com.google.cloud.bigquery.storage.v1.stream.ReadSession
import com.google.cloud.bigquery.storage.v1.DataFormat
import com.google.cloud.bigquery.storage.v1.storage.BigQueryReadClient
import com.google.cloud.bigquery.storage.v1.stream.ReadSession.TableReadOptions

import scala.concurrent.Future

/**
 * Google BigQuery Storage Api Pekko Stream operator factory using Arrow Format.
 */
object BigQueryArrowStorage {

  def readRecordsMerged(projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0): Source[Seq[BigQueryRecord], Future[NotUsed]] =
    readAndMapTo(projectId,
      datasetId,
      tableId,
      readOptions,
      maxNumStreams,
      (_, client, session) => ArrowSource.readRecordsMerged(client, session))
      .flatMapConcat(a => a)

  def readRecords(projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0): Source[Seq[Source[BigQueryRecord, NotUsed]], Future[NotUsed]] =
    readAndMapTo(projectId,
      datasetId,
      tableId,
      readOptions,
      maxNumStreams,
      (_, client, session) => ArrowSource.readRecords(client, session))

  def readMerged(projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0): Source[(ArrowSchema, Source[ArrowRecordBatch, NotUsed]), Future[NotUsed]] =
    readAndMapTo(projectId,
      datasetId,
      tableId,
      readOptions,
      maxNumStreams,
      (schema, client, session) => (schema, ArrowSource.readMerged(client, session)))

  def read(projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions] = None,
      maxNumStreams: Int = 0): Source[(ArrowSchema, Seq[Source[ArrowRecordBatch, NotUsed]]), Future[NotUsed]] =
    readAndMapTo(projectId,
      datasetId,
      tableId,
      readOptions,
      maxNumStreams,
      (schema, client, session) => (schema, ArrowSource.read(client, session)))

  private def readAndMapTo[T](projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int,
      fx: (ArrowSchema, BigQueryReadClient, ReadSession) => T): Source[T, Future[NotUsed]] =
    Source.fromMaterializer { (mat, attr) =>
      val client = reader(mat.system, attr).client
      readSession(client, projectId, datasetId, tableId, DataFormat.ARROW, readOptions, maxNumStreams)
        .map { session =>
          session.schema match {
            case ReadSession.Schema.ArrowSchema(schema) => fx(schema, client, session)
            case other                                  => throw new IllegalArgumentException(s"Only Arrow format is supported, received: $other")
          }
        }
    }

}
