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

package akka.stream.alpakka.sqs.javadsl

import java.util.concurrent.CompletionStage

import akka.Done
import akka.stream.alpakka.sqs.{ MessageAction, SqsAckGroupedSettings, SqsAckSettings }
import akka.stream.javadsl.Sink
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.compat.java8.FutureConverters.FutureOps

/**
 * Java API to create acknowledging sinks.
 */
object SqsAckSink {

  /**
   * creates a [[akka.stream.javadsl.Sink Sink]] for ack a single SQS message at a time using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def create(queueUrl: String,
      settings: SqsAckSettings,
      sqsClient: SqsAsyncClient): Sink[MessageAction, CompletionStage[Done]] =
    akka.stream.alpakka.sqs.scaladsl.SqsAckSink
      .apply(queueUrl, settings)(sqsClient)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * creates a [[akka.stream.javadsl.Sink Sink]] for ack grouped SQS messages using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def createGrouped(queueUrl: String,
      settings: SqsAckGroupedSettings,
      sqsClient: SqsAsyncClient): Sink[MessageAction, CompletionStage[Done]] =
    akka.stream.alpakka.sqs.scaladsl.SqsAckSink
      .grouped(queueUrl, settings)(sqsClient)
      .mapMaterializedValue(_.toJava)
      .asJava
}
