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

package akka.stream.alpakka.kinesisfirehose.scaladsl

import akka.NotUsed
import akka.dispatch.ExecutionContexts.parasitic
import akka.stream.ThrottleMode
import akka.stream.alpakka.kinesisfirehose.KinesisFirehoseFlowSettings
import akka.stream.alpakka.kinesisfirehose.KinesisFirehoseErrors.FailurePublishingRecords
import akka.stream.scaladsl.Flow
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient
import software.amazon.awssdk.services.firehose.model.{ PutRecordBatchRequest, PutRecordBatchResponseEntry, Record }

import scala.jdk.CollectionConverters._
import scala.collection.immutable.Queue
import scala.concurrent.duration._

import scala.compat.java8.FutureConverters._

object KinesisFirehoseFlow {
  def apply(streamName: String, settings: KinesisFirehoseFlowSettings = KinesisFirehoseFlowSettings.Defaults)(
      implicit kinesisClient: FirehoseAsyncClient): Flow[Record, PutRecordBatchResponseEntry, NotUsed] =
    Flow[Record]
      .throttle(settings.maxRecordsPerSecond, 1.second, settings.maxRecordsPerSecond, ThrottleMode.Shaping)
      .throttle(settings.maxBytesPerSecond, 1.second, settings.maxBytesPerSecond, getByteSize, ThrottleMode.Shaping)
      .batch(settings.maxBatchSize, Queue(_))(_ :+ _)
      .mapAsync(settings.parallelism)(records =>
        kinesisClient
          .putRecordBatch(
            PutRecordBatchRequest
              .builder()
              .deliveryStreamName(streamName)
              .records(records.asJavaCollection)
              .build())
          .toScala
          .transform(identity, FailurePublishingRecords(_))(parasitic))
      .mapConcat(_.requestResponses.asScala.toIndexedSeq)

  private def getByteSize(record: Record): Int = record.data.asByteBuffer.position

}
