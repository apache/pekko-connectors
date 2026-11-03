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

package org.apache.pekko.stream.connectors.kinesis.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.kinesis.{ scaladsl, KinesisFlowSettings }
import pekko.stream.javadsl.Sink
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.{ PutRecordsRequestEntry, PutRecordsResultEntry }

object KinesisSink {

  def create(streamName: String, kinesisClient: KinesisAsyncClient): Sink[PutRecordsRequestEntry, NotUsed] =
    create(streamName, KinesisFlowSettings.Defaults, kinesisClient)

  def create(streamName: String,
      settings: KinesisFlowSettings,
      kinesisClient: KinesisAsyncClient): Sink[PutRecordsRequestEntry, NotUsed] =
    scaladsl.KinesisSink(streamName, settings)(kinesisClient).asJava

  /**
   * Java API
   *
   * @since 2.0.0
   */
  def byPartitionAndData(streamName: String, kinesisClient: KinesisAsyncClient)
      : Sink[pekko.japi.Pair[String, java.nio.ByteBuffer], NotUsed] =
    byPartitionAndData(streamName, KinesisFlowSettings.Defaults, kinesisClient)

  /**
   * Java API
   *
   * @since 2.0.0
   */
  def byPartitionAndData(streamName: String,
      settings: KinesisFlowSettings,
      kinesisClient: KinesisAsyncClient): Sink[pekko.japi.Pair[String, java.nio.ByteBuffer], NotUsed] =
    KinesisFlow.byPartitionAndData(streamName, settings, kinesisClient).to(Sink.ignore[PutRecordsResultEntry]())

  /**
   * Java API
   *
   * @since 2.0.0
   */
  def byPartitionAndBytes(streamName: String, kinesisClient: KinesisAsyncClient)
      : Sink[pekko.japi.Pair[String, pekko.util.ByteString], NotUsed] =
    byPartitionAndBytes(streamName, KinesisFlowSettings.Defaults, kinesisClient)

  /**
   * Java API
   *
   * @since 2.0.0
   */
  def byPartitionAndBytes(streamName: String,
      settings: KinesisFlowSettings,
      kinesisClient: KinesisAsyncClient): Sink[pekko.japi.Pair[String, pekko.util.ByteString], NotUsed] =
    KinesisFlow.byPartitionAndBytes(streamName, settings, kinesisClient).to(Sink.ignore[PutRecordsResultEntry]())

}
