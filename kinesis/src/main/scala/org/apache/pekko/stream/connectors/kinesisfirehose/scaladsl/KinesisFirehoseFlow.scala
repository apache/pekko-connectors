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

package org.apache.pekko.stream.connectors.kinesisfirehose.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.dispatch.ExecutionContexts.parasitic
import pekko.stream.ThrottleMode
import pekko.stream.connectors.kinesisfirehose.KinesisFirehoseFlowSettings
import pekko.stream.connectors.kinesisfirehose.KinesisFirehoseErrors.FailurePublishingRecords
import pekko.stream.scaladsl.Flow
import pekko.util.FutureConverters._
import pekko.util.ccompat.JavaConverters._
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient
import software.amazon.awssdk.services.firehose.model.{ PutRecordBatchRequest, PutRecordBatchResponseEntry, Record }

import scala.collection.immutable.Queue
import scala.concurrent.duration._

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
          .asScala
          .transform(identity, FailurePublishingRecords.apply)(parasitic))
      .mapConcat(_.requestResponses.asScala.toIndexedSeq)

  private def getByteSize(record: Record): Int = record.data.asByteBuffer.position

}
