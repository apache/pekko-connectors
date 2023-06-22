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

package org.apache.pekko.stream.connectors.sqs.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.sqs.{ MessageAction, SqsAckGroupedSettings, SqsAckSettings }
import pekko.stream.javadsl.Sink
import pekko.util.FutureConverters._
import software.amazon.awssdk.services.sqs.SqsAsyncClient

/**
 * Java API to create acknowledging sinks.
 */
object SqsAckSink {

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] for ack a single SQS message at a time using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def create(queueUrl: String,
      settings: SqsAckSettings,
      sqsClient: SqsAsyncClient): Sink[MessageAction, CompletionStage[Done]] =
    pekko.stream.connectors.sqs.scaladsl.SqsAckSink
      .apply(queueUrl, settings)(sqsClient)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] for ack grouped SQS messages using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def createGrouped(queueUrl: String,
      settings: SqsAckGroupedSettings,
      sqsClient: SqsAsyncClient): Sink[MessageAction, CompletionStage[Done]] =
    pekko.stream.connectors.sqs.scaladsl.SqsAckSink
      .grouped(queueUrl, settings)(sqsClient)
      .mapMaterializedValue(_.asJava)
      .asJava
}
