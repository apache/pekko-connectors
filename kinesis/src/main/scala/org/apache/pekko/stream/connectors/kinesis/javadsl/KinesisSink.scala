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

package org.apache.pekko.stream.connectors.kinesis.javadsl

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.kinesis.{ scaladsl, KinesisFlowSettings }
import org.apache.pekko.stream.javadsl.Sink
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry

object KinesisSink {

  def create(streamName: String, kinesisClient: KinesisAsyncClient): Sink[PutRecordsRequestEntry, NotUsed] =
    create(streamName, KinesisFlowSettings.Defaults, kinesisClient)

  def create(streamName: String,
      settings: KinesisFlowSettings,
      kinesisClient: KinesisAsyncClient): Sink[PutRecordsRequestEntry, NotUsed] =
    scaladsl.KinesisSink(streamName, settings)(kinesisClient).asJava

}
