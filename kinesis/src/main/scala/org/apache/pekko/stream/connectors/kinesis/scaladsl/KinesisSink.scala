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

package org.apache.pekko.stream.connectors.kinesis.scaladsl

import java.nio.ByteBuffer

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.kinesis.KinesisFlowSettings
import pekko.stream.scaladsl.Sink
import pekko.util.ByteString
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry

object KinesisSink {

  def apply(streamName: String, settings: KinesisFlowSettings = KinesisFlowSettings.Defaults)(
      implicit kinesisClient: KinesisAsyncClient): Sink[PutRecordsRequestEntry, NotUsed] =
    KinesisFlow(streamName, settings).to(Sink.ignore)

  def byPartitionAndData(
      streamName: String,
      settings: KinesisFlowSettings = KinesisFlowSettings.Defaults)(
      implicit kinesisClient: KinesisAsyncClient): Sink[(String, ByteBuffer), NotUsed] =
    KinesisFlow.byPartitionAndData(streamName, settings).to(Sink.ignore)

  def byPartitionAndBytes(
      streamName: String,
      settings: KinesisFlowSettings = KinesisFlowSettings.Defaults)(
      implicit kinesisClient: KinesisAsyncClient): Sink[(String, ByteString), NotUsed] =
    KinesisFlow.byPartitionAndBytes(streamName, settings).to(Sink.ignore)

}
