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

package org.apache.pekko.stream.connectors.sqs.scaladsl

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.sqs.{ SqsPublishBatchSettings, SqsPublishGroupedSettings, SqsPublishSettings }
import pekko.stream.scaladsl.{ Flow, Keep, Sink }
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

import scala.concurrent.Future

/**
 * Scala API to create publishing SQS sinks.
 */
object SqsPublishSink {

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] that accepts strings and publishes them as messages to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def apply(
      queueUrl: String,
      settings: SqsPublishSettings = SqsPublishSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Sink[String, Future[Done]] =
    Flow
      .fromFunction((msg: String) => SendMessageRequest.builder().queueUrl(queueUrl).messageBody(msg).build())
      .toMat(messageSink(queueUrl, settings))(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] that groups strings and publishes them as messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   *
   * @see https://pekko.apache.org/docs/pekko/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def grouped(queueUrl: String, settings: SqsPublishGroupedSettings = SqsPublishGroupedSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Sink[String, Future[Done]] =
    Flow
      .fromFunction((msg: String) => SendMessageRequest.builder().queueUrl(queueUrl).messageBody(msg).build())
      .toMat(groupedMessageSink(queueUrl, settings))(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] that accepts an iterable of strings and publish them as messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   *
   * @see https://pekko.apache.org/docs/pekko/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def batch(
      queueUrl: String,
      settings: SqsPublishBatchSettings = SqsPublishBatchSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Sink[Iterable[String], Future[Done]] =
    Flow
      .fromFunction((msgs: Iterable[String]) =>
        msgs.map(msg => SendMessageRequest.builder().queueUrl(queueUrl).messageBody(msg).build()))
      .toMat(batchedMessageSink(queueUrl, settings))(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def messageSink(
      queueUrl: String,
      settings: SqsPublishSettings)(implicit sqsClient: SqsAsyncClient): Sink[SendMessageRequest, Future[Done]] =
    SqsPublishFlow.apply(queueUrl, settings).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def messageSink(queueUrl: String)(implicit sqsClient: SqsAsyncClient): Sink[SendMessageRequest, Future[Done]] =
    SqsPublishFlow.apply(queueUrl, SqsPublishSettings.Defaults).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to SQS queues based on the message queue url using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def messageSink(
      settings: SqsPublishSettings = SqsPublishSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Sink[SendMessageRequest, Future[Done]] =
    SqsPublishFlow.apply(settings).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] that groups messages and publishes them in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   *
   * @see https://pekko.apache.org/docs/pekko/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def groupedMessageSink(
      queueUrl: String,
      settings: SqsPublishGroupedSettings = SqsPublishGroupedSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Sink[SendMessageRequest, Future[Done]] =
    SqsPublishFlow.grouped(queueUrl, settings).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def batchedMessageSink(
      queueUrl: String,
      settings: SqsPublishBatchSettings = SqsPublishBatchSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Sink[Iterable[SendMessageRequest], Future[Done]] =
    SqsPublishFlow.batch(queueUrl, settings).toMat(Sink.ignore)(Keep.right)
}
