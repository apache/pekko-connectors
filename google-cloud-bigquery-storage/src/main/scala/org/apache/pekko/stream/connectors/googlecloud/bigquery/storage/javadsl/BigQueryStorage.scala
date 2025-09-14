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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.http.javadsl.unmarshalling.Unmarshaller
import pekko.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import pekko.stream.connectors.googlecloud.bigquery.storage.ProtobufConverters._
import pekko.stream.connectors.googlecloud.bigquery.storage.{ scaladsl => scstorage }
import pekko.stream.javadsl.Source
import pekko.util.ByteString
import pekko.util.FutureConverters._
import com.google.cloud.bigquery.storage.v1.DataFormat
import com.google.cloud.bigquery.storage.v1.ReadSession.TableReadOptions
import com.google.cloud.bigquery.storage.v1.storage.ReadRowsResponse
import com.google.cloud.bigquery.storage.v1.stream.ReadSession

import java.util.concurrent.CompletionStage

import scala.jdk.CollectionConverters._

/**
 * Google BigQuery Storage Api Pekko Stream operator factory.
 */
object BigQueryStorage {

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   *
   * @param projectId  the projectId the table is located in
   * @param datasetId  the datasetId the table is located in
   * @param tableId    the table to query
   * @param dataFormat the format to Receive the data
   */
  def create(
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat)
      : Source[(ReadSession.Schema, java.util.List[Source[ReadRowsResponse.Rows, NotUsed]]), CompletionStage[NotUsed]] =
    create(projectId, datasetId, tableId, dataFormat, None, 1)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   * These sources will emit one GenericRecord for each row within that stream.
   *
   * @param projectId   the projectId the table is located in
   * @param datasetId   the datasetId the table is located in
   * @param tableId     the table to query
   * @param dataFormat  the format to Receive the data
   * @param readOptions TableReadOptions to reduce the amount of data to return, either by column projection or filtering
   */
  def create(
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: TableReadOptions)
      : Source[(ReadSession.Schema, java.util.List[Source[ReadRowsResponse.Rows, NotUsed]]), CompletionStage[NotUsed]] =
    create(projectId, datasetId, tableId, dataFormat, Some(readOptions), 1)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   *
   * @param projectId     the projectId the table is located in
   * @param datasetId     the datasetId the table is located in
   * @param tableId       the table to query
   * @param dataFormat    the format to Receive the data
   * @param readOptions   TableReadOptions to reduce the amount of data to return, either by column projection or filtering
   * @param maxNumStreams An optional max initial number of streams. If unset or zero, the server will provide a value of streams so as to produce reasonable throughput.
   *                      Must be non-negative. The number of streams may be lower than the requested number, depending on the amount parallelism that is reasonable for the table.
   *                      Error will be returned if the max count is greater than the current system max limit of 1,000.
   */
  def create(
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: TableReadOptions,
      maxNumStreams: Int)
      : Source[(ReadSession.Schema, java.util.List[Source[ReadRowsResponse.Rows, NotUsed]]), CompletionStage[NotUsed]] =
    create(projectId, datasetId, tableId, dataFormat, Some(readOptions), maxNumStreams)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   *
   * @param projectId     the projectId the table is located in
   * @param datasetId     the datasetId the table is located in
   * @param tableId       the table to query
   * @param dataFormat    the format to Receive the data
   * @param maxNumStreams An optional max initial number of streams. If unset or zero, the server will provide a value of streams so as to produce reasonable throughput.
   *                      Must be non-negative. The number of streams may be lower than the requested number, depending on the amount parallelism that is reasonable for the table.
   *                      Error will be returned if the max count is greater than the current system max limit of 1,000.
   */
  def create(
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      maxNumStreams: Int)
      : Source[(ReadSession.Schema, java.util.List[Source[ReadRowsResponse.Rows, NotUsed]]), CompletionStage[NotUsed]] =
    create(projectId, datasetId, tableId, dataFormat, None, maxNumStreams)

  private[this] def create(
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int)
      : Source[(ReadSession.Schema, java.util.List[Source[ReadRowsResponse.Rows, NotUsed]]), CompletionStage[NotUsed]] =
    scstorage.BigQueryStorage
      .create(projectId, datasetId, tableId, dataFormat, readOptions.map(_.asScala()), maxNumStreams)
      .map(stream => {
        (stream._1, stream._2.map(_.asJava).asJava)
      })
      .asJava
      .mapMaterializedValue(_.asJava)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   * These sources will emit one GenericRecord for each row within that stream.
   *
   * @param projectId  the projectId the table is located in
   * @param datasetId  the datasetId the table is located in
   * @param tableId    the table to query
   * @param dataFormat the format to Receive the data
   */
  def createMergedStreams[A](projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      um: Unmarshaller[ByteString, A]): Source[A, CompletionStage[NotUsed]] =
    createMergedStreams(projectId, datasetId, tableId, dataFormat, None, 1, um.asScala)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   * These sources will emit one GenericRecord for each row within that stream.
   *
   * @param projectId   the projectId the table is located in
   * @param datasetId   the datasetId the table is located in
   * @param tableId     the table to query
   * @param dataFormat  the format to Receive the data
   * @param readOptions TableReadOptions to reduce the amount of data to return, either by column projection or filtering
   */
  def createMergedStreams[A](projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: TableReadOptions,
      um: Unmarshaller[ByteString, A]): Source[A, CompletionStage[NotUsed]] =
    createMergedStreams(projectId, datasetId, tableId, dataFormat, Some(readOptions), 0, um.asScala)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   * These sources will emit one GenericRecord for each row within that stream.
   *
   * @param projectId     the projectId the table is located in
   * @param datasetId     the datasetId the table is located in
   * @param tableId       the table to query
   * @param dataFormat    the format to Receive the data
   * @param readOptions   TableReadOptions to reduce the amount of data to return, either by column projection or filtering
   * @param maxNumStreams An optional max initial number of streams. If unset or zero, the server will provide a value of streams so as to produce reasonable throughput.
   *                      Must be non-negative. The number of streams may be lower than the requested number, depending on the amount parallelism that is reasonable for the table.
   *                      Error will be returned if the max count is greater than the current system max limit of 1,000.
   */
  def createMergedStreams[A](projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: TableReadOptions,
      maxNumStreams: Int,
      um: Unmarshaller[ByteString, A]): Source[A, CompletionStage[NotUsed]] =
    createMergedStreams(projectId, datasetId, tableId, dataFormat, Some(readOptions), maxNumStreams, um.asScala)

  /**
   * Create a source that contains a number of sources, one for each stream, or section of the table data.
   * These sources will emit one GenericRecord for each row within that stream.
   *
   * @param projectId     the projectId the table is located in
   * @param datasetId     the datasetId the table is located in
   * @param tableId       the table to query
   * @param dataFormat    the format to Receive the data
   * @param maxNumStreams An optional max initial number of streams. If unset or zero, the server will provide a value of streams so as to produce reasonable throughput.
   *                      Must be non-negative. The number of streams may be lower than the requested number, depending on the amount parallelism that is reasonable for the table.
   *                      Error will be returned if the max count is greater than the current system max limit of 1,000.
   * @param um            the By
   */
  def createMergedStreams[A](projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      maxNumStreams: Int,
      um: Unmarshaller[ByteString, A]): Source[A, CompletionStage[NotUsed]] =
    createMergedStreams(projectId, datasetId, tableId, dataFormat, None, maxNumStreams, um.asScala)

  private[this] def createMergedStreams[A](
      projectId: String,
      datasetId: String,
      tableId: String,
      dataFormat: DataFormat,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int,
      um: FromByteStringUnmarshaller[A]): Source[A, CompletionStage[NotUsed]] =
    scstorage.BigQueryStorage
      .createMergedStreams(projectId, datasetId, tableId, dataFormat, readOptions.map(_.asScala()), maxNumStreams)(um)
      .asJava
      .mapMaterializedValue(_.asJava)

}
