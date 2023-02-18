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

package akka.stream.alpakka.kinesis.javadsl

import akka.NotUsed
import akka.stream.alpakka.kinesis.{ scaladsl, KinesisFlowSettings }
import akka.stream.javadsl.Flow
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.{ PutRecordsRequestEntry, PutRecordsResultEntry }
import akka.stream.javadsl.FlowWithContext

object KinesisFlow {

  def create(streamName: String,
      kinesisClient: KinesisAsyncClient): Flow[PutRecordsRequestEntry, PutRecordsResultEntry, NotUsed] =
    create(streamName, KinesisFlowSettings.Defaults, kinesisClient)

  def create(streamName: String,
      settings: KinesisFlowSettings,
      kinesisClient: KinesisAsyncClient): Flow[PutRecordsRequestEntry, PutRecordsResultEntry, NotUsed] =
    scaladsl.KinesisFlow
      .apply(streamName, settings)(kinesisClient)
      .asJava

  def createWithContext[T](
      streamName: String,
      kinesisClient: KinesisAsyncClient)
      : FlowWithContext[PutRecordsRequestEntry, T, PutRecordsResultEntry, T, NotUsed] =
    createWithContext(streamName, KinesisFlowSettings.Defaults, kinesisClient)

  def createWithContext[T](
      streamName: String,
      settings: KinesisFlowSettings,
      kinesisClient: KinesisAsyncClient)
      : FlowWithContext[PutRecordsRequestEntry, T, PutRecordsResultEntry, T, NotUsed] =
    akka.stream.scaladsl
      .FlowWithContext[PutRecordsRequestEntry, T]
      .via(scaladsl.KinesisFlow.withContext[T](streamName, settings)(kinesisClient))
      .asJava
}
