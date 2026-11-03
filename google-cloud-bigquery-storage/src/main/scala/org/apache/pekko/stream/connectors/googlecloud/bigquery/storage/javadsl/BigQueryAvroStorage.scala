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
import pekko.japi.Pair
import pekko.stream.connectors.googlecloud.bigquery.storage.ProtobufConverters._
import pekko.stream.connectors.googlecloud.bigquery.storage.{ scaladsl => scstorage, BigQueryRecord }
import pekko.stream.javadsl.Source
import com.google.cloud.bigquery.storage.v1.avro.{ AvroRows, AvroSchema }
import com.google.cloud.bigquery.storage.v1.ReadSession.{ TableReadOptions => JavaTableReadOptions }
import com.google.cloud.bigquery.storage.v1.stream.ReadSession.TableReadOptions

import java.util.concurrent.CompletionStage

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

/**
 * Google BigQuery Storage Api Pekko Stream operator factory using Avro Format.
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
      .mapMaterializedValue(_.asJava)

  def readRecords(projectId: String,
      datasetId: String,
      tableId: String): Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId, None, 0)

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
      .mapMaterializedValue(_.asJava)

  @deprecated("Use readMergedAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
  def readMerged(projectId: String,
      datasetId: String,
      tableId: String): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, None, 0)

  @deprecated("Use readMergedAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
  def readMerged(projectId: String,
      datasetId: String,
      tableId: String,
      maxNumStreams: Int): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, None, maxNumStreams)

  @deprecated("Use readMergedAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
  def readMerged(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions): Source[(AvroSchema, Source[AvroRows, NotUsed]), CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, Some(readOptions), 0)

  @deprecated("Use readMergedAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
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
      .mapMaterializedValue(_.asJava)

  @deprecated("Use readAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
  def read(projectId: String,
      datasetId: String,
      tableId: String): Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, None, 0)

  @deprecated("Use readAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
  def read(
      projectId: String,
      datasetId: String,
      tableId: String,
      maxNumStreams: Int): Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, None, maxNumStreams)

  @deprecated("Use readAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
  def read(
      projectId: String,
      datasetId: String,
      tableId: String,
      readOptions: TableReadOptions)
      : Source[(AvroSchema, java.util.List[Source[AvroRows, NotUsed]]), CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, Some(readOptions), 0)

  @deprecated("Use readAsPair, which emits a org.apache.pekko.japi.Pair", "2.0.0")
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
      .mapMaterializedValue(_.asJava)

  /**
   * The same as the overload taking the ScalaPB `TableReadOptions`, for the Java protobuf type.
   *
   * @since 2.0.0
   */
  def readRecordsMerged(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions)
      : Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    readRecordsMerged(projectId, datasetId, tableId, readOptions.asScala())

  /**
   * The same as the overload taking the ScalaPB `TableReadOptions`, for the Java protobuf type.
   *
   * @since 2.0.0
   */
  def readRecordsMerged(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions,
      maxNumStreams: Int): Source[java.util.List[BigQueryRecord], CompletionStage[NotUsed]] =
    readRecordsMerged(projectId, datasetId, tableId, readOptions.asScala(), maxNumStreams)

  /**
   * The same as the overload taking the ScalaPB `TableReadOptions`, for the Java protobuf type.
   *
   * @since 2.0.0
   */
  def readRecords(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions)
      : Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId, readOptions.asScala())

  /**
   * The same as the overload taking the ScalaPB `TableReadOptions`, for the Java protobuf type.
   *
   * @since 2.0.0
   */
  def readRecords(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions,
      maxNumStreams: Int): Source[java.util.List[Source[BigQueryRecord, NotUsed]], CompletionStage[NotUsed]] =
    readRecords(projectId, datasetId, tableId, readOptions.asScala(), maxNumStreams)

  /**
   * The same as `readMerged`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readMergedAsPair(projectId: String, datasetId: String, tableId: String)
      : Source[Pair[AvroSchema, Source[AvroRows, NotUsed]], CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `readMerged`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readMergedAsPair(projectId: String, datasetId: String, tableId: String, maxNumStreams: Int)
      : Source[Pair[AvroSchema, Source[AvroRows, NotUsed]], CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, maxNumStreams).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `readMerged`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readMergedAsPair(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions)
      : Source[Pair[AvroSchema, Source[AvroRows, NotUsed]], CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, readOptions.asScala()).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `readMerged`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readMergedAsPair(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions,
      maxNumStreams: Int): Source[Pair[AvroSchema, Source[AvroRows, NotUsed]], CompletionStage[NotUsed]] =
    readMerged(projectId, datasetId, tableId, readOptions.asScala(), maxNumStreams).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `read`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readAsPair(projectId: String, datasetId: String, tableId: String)
      : Source[Pair[AvroSchema, java.util.List[Source[AvroRows, NotUsed]]], CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `read`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readAsPair(projectId: String, datasetId: String, tableId: String, maxNumStreams: Int)
      : Source[Pair[AvroSchema, java.util.List[Source[AvroRows, NotUsed]]], CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, maxNumStreams).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `read`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readAsPair(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions)
      : Source[Pair[AvroSchema, java.util.List[Source[AvroRows, NotUsed]]], CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, readOptions.asScala()).map(t => Pair.create(t._1, t._2))

  /**
   * The same as `read`, emitting a [[pekko.japi.Pair]] instead of a Scala tuple.
   *
   * @since 2.0.0
   */
  @nowarn("cat=deprecation")
  def readAsPair(projectId: String, datasetId: String, tableId: String, readOptions: JavaTableReadOptions,
      maxNumStreams: Int)
      : Source[Pair[AvroSchema, java.util.List[Source[AvroRows, NotUsed]]], CompletionStage[NotUsed]] =
    read(projectId, datasetId, tableId, readOptions.asScala(), maxNumStreams).map(t => Pair.create(t._1, t._2))
}
