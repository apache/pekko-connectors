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

package org.apache.pekko.stream.connectors.sqs.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.sqs._
import pekko.stream.javadsl.Sink
import pekko.stream.scaladsl.{ Flow, Keep }
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters.FutureOps

/**
 * Java API to create SQS Sinks.
 */
object SqsPublishSink {

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] that accepts strings and publishes them as messages to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def create(queueUrl: String,
      settings: SqsPublishSettings,
      sqsClient: SqsAsyncClient): Sink[String, CompletionStage[Done]] =
    scaladsl.SqsPublishSink.apply(queueUrl, settings)(sqsClient).mapMaterializedValue(_.toJava).asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] to publish messages to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def messageSink(queueUrl: String,
      settings: SqsPublishSettings,
      sqsClient: SqsAsyncClient): Sink[SendMessageRequest, CompletionStage[Done]] =
    scaladsl.SqsPublishSink
      .messageSink(queueUrl, settings)(sqsClient)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] to publish messages to SQS queues based on the message queue url using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def messageSink(settings: SqsPublishSettings,
      sqsClient: SqsAsyncClient): Sink[SendMessageRequest, CompletionStage[Done]] =
    scaladsl.SqsPublishSink
      .messageSink(settings)(sqsClient)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] that groups strings and publishes them as messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   * @see https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def grouped(queueUrl: String,
      settings: SqsPublishGroupedSettings,
      sqsClient: SqsAsyncClient): Sink[String, CompletionStage[Done]] =
    scaladsl.SqsPublishSink.grouped(queueUrl, settings)(sqsClient).mapMaterializedValue(_.toJava).asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] that groups messages and publishes them in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   * @see https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def groupedMessageSink(queueUrl: String,
      settings: SqsPublishGroupedSettings,
      sqsClient: SqsAsyncClient): Sink[SendMessageRequest, CompletionStage[Done]] =
    scaladsl.SqsPublishSink
      .groupedMessageSink(queueUrl, settings)(sqsClient)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] that accepts an iterable of strings and publish them as messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   * @see https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def batch[B <: java.lang.Iterable[String]](queueUrl: String,
      settings: SqsPublishBatchSettings,
      sqsClient: SqsAsyncClient): Sink[B, CompletionStage[Done]] =
    Flow[java.lang.Iterable[String]]
      .map(_.asScala)
      .toMat(scaladsl.SqsPublishSink.batch(queueUrl, settings)(sqsClient))(Keep.right)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * creates a [[pekko.stream.javadsl.Sink Sink]] to publish messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def batchedMessageSink[B <: java.lang.Iterable[SendMessageRequest]](
      queueUrl: String,
      settings: SqsPublishBatchSettings,
      sqsClient: SqsAsyncClient): Sink[B, CompletionStage[Done]] =
    Flow[java.lang.Iterable[SendMessageRequest]]
      .map(_.asScala)
      .toMat(scaladsl.SqsPublishSink.batchedMessageSink(queueUrl, settings)(sqsClient))(Keep.right)
      .mapMaterializedValue(_.toJava)
      .asJava
}
