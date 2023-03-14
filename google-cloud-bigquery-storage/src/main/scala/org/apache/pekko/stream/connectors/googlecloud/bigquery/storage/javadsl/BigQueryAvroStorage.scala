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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.{ scaladsl => scstorage, BigQueryRecord }
import org.apache.pekko.stream.javadsl.Source
import com.google.cloud.bigquery.storage.v1.avro.{ AvroRows, AvroSchema }
import com.google.cloud.bigquery.storage.v1.stream.ReadSession.TableReadOptions

import java.util.concurrent.CompletionStage
import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters.FutureOps

/**
 * Google BigQuery Storage Api Akka Stream operator factory using Avro Format.
 */
object BigQueryAvroStorage {

  def readRecordsMerged(projectId: String,
      datasetId: String,
      tableId: String): Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    readRecordsMerged(projectId, datasetId, tableId, None, 0)

  def readRecordsMerged(projectId: String,
      datasetId: String,
      tableId: String,
      maxNumStreams: Int): Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    readRecordsMerged(projectId, datasetId, tableId, None, maxNumStreams)

  def readRecordsMerged(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions): Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    readRecordsMerged(projectId, datasetId, tableId, Some(readOptions), 0)

  def readRecordsMerged(projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions,
      maxNumStreams: Int): Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    readRecordsMerged(projectId, datasetId, tableId, Some(readOptions), maxNumStreams)

  private def readRecordsMerged(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int): Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    scstorage.BigQueryAvroStorage
      .readRecordsMerged(projectId, datasetId, tableId, readOptions, maxNumStreams)
      .map(stream => {
        stream.asJava
      })
      .asJava
      .mapMaterializedValue(_.toJava)

  def readRecords(projectId: String,
      datasetId: String,
      tableId: String): Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId)

  def readRecords(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions)
      : Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId, Some(readOptions), 0)

  def readRecords(
      projectId: String,
      datasetId: String,
      tableId: String,
      maxNumStreams: Int): Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId, None, maxNumStreams)

  def readRecords(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions,
      maxNumStreams: Int): Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId, Some(readOptions), maxNumStreams)

  private def readRecords(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int): Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    scstorage.BigQueryAvroStorage
      .readRecords(projectId, datasetId, tableId, readOptions, maxNumStreams)
      .map(stream => {
        stream.map(_.asJava).asJava
      })
      .asJava
      .mapMaterializedValue(_.toJava)

  def readMerged(projectId: String,
      datasetId: String,
      tableId: String): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId)

  def readMerged(projectId: String,
      datasetId: String,
      tableId: String,
      maxNumStreams: Int): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, None, maxNumStreams)

  def readMerged(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, Some(readOptions), 0)

  def readMerged(projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions,
      maxNumStreams: Int): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, Some(readOptions), maxNumStreams)

  private def readMerged(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    scstorage.BigQueryAvroStorage
      .readMerged(projectId, datasetId, tableId, readOptions, maxNumStreams)
      .map(stream => {
        (stream._1, stream._2.asJava)
      })
      .asJava
      .mapMaterializedValue(_.toJava)

  def read(projectId: String,
      datasetId: String,
      tableId: String): Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, None, 0)

  def read(
      projectId: String,
      datasetId: String,
      tableId: String,
      maxNumStreams: Int): Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, None, maxNumStreams)

  def read(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions)
      : Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, Some(readOptions), 0)

  def read(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions,
      maxNumStreams: Int): Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, Some(readOptions), maxNumStreams)

  private def read(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: Option[TableReadOptions],
      maxNumStreams: Int): Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    scstorage.BigQueryAvroStorage
      .read(projectId, datasetId, tableId, readOptions, maxNumStreams)
      .map(stream => {
        (stream._1, stream._2.map(_.asJava).asJava)
      })
      .asJava
      .mapMaterializedValue(_.toJava)

}
