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

package org.apache.pekko.stream.connectors.sns.scaladsl

import org.apache.pekko
import pekko.stream.connectors.sns.SnsPublishSettings
import pekko.stream.scaladsl.{ Flow, Keep, Sink }
import pekko.{ Done, NotUsed }
import pekko.util.FutureConverters._
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{ PublishRequest, PublishResponse }

import scala.concurrent.Future

/**
 * Scala API
 * Amazon SNS publisher factory.
 */
object SnsPublisher {

  /**
   * creates a [[pekko.stream.scaladsl.Flow Flow]] to publish messages to a SNS topic using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def flow(topicArn: String, settings: SnsPublishSettings = SnsPublishSettings())(
      implicit snsClient: SnsAsyncClient): Flow[String, PublishResponse, NotUsed] =
    Flow
      .fromFunction((message: String) => PublishRequest.builder().message(message).topicArn(topicArn).build())
      .via(publishFlow(settings))

  /**
   * creates a [[pekko.stream.scaladsl.Flow Flow]] to publish messages to a SNS topic using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def publishFlow(topicArn: String, settings: SnsPublishSettings = SnsPublishSettings())(
      implicit snsClient: SnsAsyncClient): Flow[PublishRequest, PublishResponse, NotUsed] =
    Flow
      .fromFunction((request: PublishRequest) => request.toBuilder.topicArn(topicArn).build())
      .via(publishFlow(settings))

  /**
   * creates a [[pekko.stream.scaladsl.Flow Flow]] to publish messages to SNS topics based on the message topic arn using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def publishFlow(
      settings: SnsPublishSettings)(
      implicit snsClient: SnsAsyncClient): Flow[PublishRequest, PublishResponse, NotUsed] = {
    require(snsClient != null, "The `SnsAsyncClient` passed in may not be null.")
    Flow[PublishRequest]
      .mapAsyncUnordered(settings.concurrency)(snsClient.publish(_).asScala)
  }

  /**
   * creates a [[pekko.stream.scaladsl.Flow Flow]] to publish messages to SNS topics based on the message topic arn using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def publishFlow()(implicit snsClient: SnsAsyncClient): Flow[PublishRequest, PublishResponse, NotUsed] =
    publishFlow(SnsPublishSettings())

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to a SNS topic using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def sink(topicArn: String, settings: SnsPublishSettings = SnsPublishSettings())(
      implicit snsClient: SnsAsyncClient): Sink[String, Future[Done]] =
    flow(topicArn, settings).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to a SNS topic using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def publishSink(topicArn: String, settings: SnsPublishSettings = SnsPublishSettings())(
      implicit snsClient: SnsAsyncClient): Sink[PublishRequest, Future[Done]] =
    publishFlow(topicArn, settings).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to SNS topics based on the message topic arn using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def publishSink(
      settings: SnsPublishSettings)(implicit snsClient: SnsAsyncClient): Sink[PublishRequest, Future[Done]] =
    publishFlow(settings).toMat(Sink.ignore)(Keep.right)

  /**
   * creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to SNS topics based on the message topic arn using an [[software.amazon.awssdk.services.sns.SnsAsyncClient SnsAsyncClient]]
   */
  def publishSink()(implicit snsClient: SnsAsyncClient): Sink[PublishRequest, Future[Done]] =
    publishFlow(SnsPublishSettings()).toMat(Sink.ignore)(Keep.right)
}
