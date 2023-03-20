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

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.sqs.{
  SqsPublishBatchSettings,
  SqsPublishGroupedSettings,
  SqsPublishResult,
  SqsPublishResultEntry,
  SqsPublishSettings
}
import pekko.stream.javadsl.Flow
import pekko.stream.scaladsl.{ Flow => SFlow }
import pekko.util.ccompat.JavaConverters._
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * Java API to create SQS flows.
 */
@ApiMayChange
object SqsPublishFlow {

  /**
   * creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient AmazonSQSAsync]]
   */
  def create(queueUrl: String,
      settings: SqsPublishSettings,
      sqsClient: SqsAsyncClient): Flow[SendMessageRequest, SqsPublishResult, NotUsed] =
    pekko.stream.connectors.sqs.scaladsl.SqsPublishFlow.apply(queueUrl, settings)(sqsClient).asJava

  /**
   * creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to SQS queues based on the message queue url using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient AmazonSQSAsync]]
   */
  def create(settings: SqsPublishSettings,
      sqsClient: SqsAsyncClient): Flow[SendMessageRequest, SqsPublishResult, NotUsed] =
    pekko.stream.connectors.sqs.scaladsl.SqsPublishFlow.apply(settings)(sqsClient).asJava

  /**
   * creates a [[pekko.stream.javadsl.Flow Flow]] that groups messages and publish them in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient AmazonSQSAsync]]
   * @see https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/groupedWithin.html#groupedwithin
   */
  def grouped(
      queueUrl: String,
      settings: SqsPublishGroupedSettings,
      sqsClient: SqsAsyncClient): Flow[SendMessageRequest, SqsPublishResultEntry, NotUsed] =
    pekko.stream.connectors.sqs.scaladsl.SqsPublishFlow
      .grouped(queueUrl, settings)(sqsClient)
      .asJava

  /**
   * creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages in batches to a SQS queue using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient AmazonSQSAsync]]
   */
  def batch[B <: java.lang.Iterable[SendMessageRequest]](
      queueUrl: String,
      settings: SqsPublishBatchSettings,
      sqsClient: SqsAsyncClient): Flow[B, java.util.List[SqsPublishResultEntry], NotUsed] =
    SFlow[java.lang.Iterable[SendMessageRequest]]
      .map(_.asScala)
      .via(pekko.stream.connectors.sqs.scaladsl.SqsPublishFlow.batch(queueUrl, settings)(sqsClient))
      .map(_.asJava)
      .asJava
}
