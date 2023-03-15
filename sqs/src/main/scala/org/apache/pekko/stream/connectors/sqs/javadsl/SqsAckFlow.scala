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
import pekko.stream.connectors.sqs._
import pekko.stream.javadsl.Flow
import software.amazon.awssdk.services.sqs.SqsAsyncClient

/**
 * Java API to create acknowledging SQS flows.
 */
@ApiMayChange
object SqsAckFlow {

  /**
   * creates a [[pekko.stream.javadsl.Flow Flow]] for ack a single SQS message at a time using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def create(queueUrl: String,
      settings: SqsAckSettings,
      sqsClient: SqsAsyncClient): Flow[MessageAction, SqsAckResult, NotUsed] =
    pekko.stream.connectors.sqs.scaladsl.SqsAckFlow.apply(queueUrl, settings)(sqsClient).asJava

  /**
   * creates a [[pekko.stream.javadsl.Flow Flow]] for ack grouped SQS messages using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def grouped(queueUrl: String,
      settings: SqsAckGroupedSettings,
      sqsClient: SqsAsyncClient): Flow[MessageAction, SqsAckResultEntry, NotUsed] =
    pekko.stream.connectors.sqs.scaladsl.SqsAckFlow.grouped(queueUrl, settings)(sqsClient).asJava
}
