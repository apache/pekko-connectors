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

package akka.stream.alpakka.kinesisfirehose.javadsl

import akka.NotUsed
import akka.stream.alpakka.kinesisfirehose.{ scaladsl, KinesisFirehoseFlowSettings }
import akka.stream.javadsl.Sink
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient
import software.amazon.awssdk.services.firehose.model.Record

object KinesisFirehoseSink {

  def apply(streamName: String, kinesisClient: FirehoseAsyncClient): Sink[Record, NotUsed] =
    apply(streamName, KinesisFirehoseFlowSettings.Defaults, kinesisClient)

  def apply(streamName: String,
      settings: KinesisFirehoseFlowSettings,
      kinesisClient: FirehoseAsyncClient): Sink[Record, NotUsed] =
    scaladsl.KinesisFirehoseSink.apply(streamName, settings)(kinesisClient).asJava

}
