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

package org.apache.pekko.stream.connectors.aws.eventbridge.scaladsl

import org.apache.pekko
import pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings
import pekko.stream.scaladsl.{ Flow, Keep, Sink }
import pekko.{ Done, NotUsed }
import pekko.util.FutureConverters._
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient
import software.amazon.awssdk.services.eventbridge.model._

import scala.concurrent.Future

/**
 * Scala API
 * Amazon Event Bridge publisher factory.
 */
object EventBridgePublisher {

  /**
   * Creates a [[pekko.stream.scaladsl.Flow Flow]] to publish a message to an EventBridge specified in the entry payload
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flow(settings: EventBridgePublishSettings = EventBridgePublishSettings())(
      implicit eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequestEntry, PutEventsResponse, NotUsed] =
    Flow
      .fromFunction((message: PutEventsRequestEntry) => PutEventsRequest.builder().entries(message).build())
      .via(publishFlow(settings))

  /**
   * Creates a [[pekko.stream.scaladsl.Flow Flow]] to publish a sequence of PutRequestEntries messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flowSeq(settings: EventBridgePublishSettings = EventBridgePublishSettings())(
      implicit eventBridgeClient: EventBridgeAsyncClient)
      : Flow[Seq[PutEventsRequestEntry], PutEventsResponse, NotUsed] =
    Flow
      .fromFunction((messages: Seq[PutEventsRequestEntry]) => PutEventsRequest.builder().entries(messages: _*).build())
      .via(publishFlow(settings))

  /**
   * Creates a [[pekko.stream.scaladsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishFlow(
      settings: EventBridgePublishSettings)(
      implicit eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequest, PutEventsResponse, NotUsed] =
    Flow[PutEventsRequest]
      .mapAsync(settings.concurrency)(eventBridgeClient.putEvents(_).asScala)

  /**
   * Creates a [[pekko.stream.scaladsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishFlow()(
      implicit eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequest, PutEventsResponse, NotUsed] =
    publishFlow(EventBridgePublishSettings())

  /**
   * Creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def sink(settings: EventBridgePublishSettings = EventBridgePublishSettings())(
      implicit eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequestEntry, Future[Done]] =
    flow(settings).toMat(Sink.ignore)(Keep.right)

  /**
   * Creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishSink(
      settings: EventBridgePublishSettings = EventBridgePublishSettings())(
      implicit eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequest, Future[Done]] =
    publishFlow(settings).toMat(Sink.ignore)(Keep.right)

  /**
   * Creates a [[pekko.stream.scaladsl.Sink Sink]] to publish messages to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishSink()(implicit eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequest, Future[Done]] =
    publishFlow(EventBridgePublishSettings()).toMat(Sink.ignore)(Keep.right)
}
