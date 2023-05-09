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

package org.apache.pekko.stream.connectors.sqs.scaladsl

import org.apache.pekko
import pekko._
import pekko.stream._
import pekko.stream.connectors.sqs.SqsSourceSettings
import pekko.stream.connectors.sqs.impl.BalancingMapAsync
import pekko.stream.scaladsl.{ Flow, Source }
import pekko.util.ccompat.JavaConverters._
import pekko.util.FutureConverters._
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

/**
 * Scala API to create SQS sources.
 */
object SqsSource {

  /**
   * creates a [[pekko.stream.scaladsl.Source Source]] for a SQS queue using [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def apply(
      queueUrl: String,
      settings: SqsSourceSettings = SqsSourceSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Source[Message, NotUsed] = {
    SqsAckFlow.checkClient(sqsClient)
    Source
      .repeat {
        val requestBuilder =
          ReceiveMessageRequest
            .builder()
            .queueUrl(queueUrl)
            .attributeNamesWithStrings(settings.attributeNames.map(_.name).asJava)
            .messageAttributeNames(settings.messageAttributeNames.map(_.name).asJava)
            .maxNumberOfMessages(settings.maxBatchSize)
            .waitTimeSeconds(settings.waitTimeSeconds)

        settings.visibilityTimeout match {
          case None    => requestBuilder.build()
          case Some(t) => requestBuilder.visibilityTimeout(t.toSeconds.toInt).build()
        }
      }
      .via(resolveHandler(settings.parallelRequests))
      .map(_.messages().asScala.toList)
      .takeWhile(messages => !settings.closeOnEmptyReceive || messages.nonEmpty)
      .mapConcat(identity)
      .buffer(settings.maxBufferSize, OverflowStrategy.backpressure)
  }

  private def resolveHandler(parallelism: Int)(implicit sqsClient: SqsAsyncClient) =
    if (parallelism == 1) {
      Flow[ReceiveMessageRequest].mapAsyncUnordered(parallelism)(sqsClient.receiveMessage(_).asScala)
    } else {
      BalancingMapAsync[ReceiveMessageRequest, ReceiveMessageResponse](
        parallelism,
        sqsClient.receiveMessage(_).asScala,
        (response, _) => if (response.messages().isEmpty) 1 else parallelism)
    }
}
