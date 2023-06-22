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

package org.apache.pekko.stream.connectors.aws.eventbridge.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings
import pekko.stream.javadsl.{ Flow, Keep, Sink }
import pekko.{ Done, NotUsed }
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient
import software.amazon.awssdk.services.eventbridge.model._

/**
 * Java API
 * Amazon EventBridge publisher factory.
 */
object EventBridgePublisher {

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flow(settings: EventBridgePublishSettings,
      eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequestEntry, PutEventsResponse, NotUsed] =
    pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher.flow(settings)(
      eventBridgeClient).asJava

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flow(eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequestEntry, PutEventsResponse, NotUsed] =
    pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher
      .flow(EventBridgePublishSettings())(eventBridgeClient)
      .asJava

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flowSeq(
      settings: EventBridgePublishSettings,
      eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequestEntry, PutEventsResponse, NotUsed] =
    pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher.flow(settings)(
      eventBridgeClient).asJava

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flowSeq(
      eventBridgeClient: EventBridgeAsyncClient): Flow[Seq[PutEventsRequestEntry], PutEventsResponse, NotUsed] =
    pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher
      .flowSeq(EventBridgePublishSettings())(eventBridgeClient)
      .asJava

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishFlow(settings: EventBridgePublishSettings,
      eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequest, PutEventsResponse, NotUsed] =
    pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher.publishFlow(settings)(
      eventBridgeClient).asJava

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish a message to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishFlow(eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequest, PutEventsResponse, NotUsed] =
    pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher
      .publishFlow(EventBridgePublishSettings())(eventBridgeClient)
      .asJava

  /**
   * Creates a [[pekko.stream.javadsl.Sink Sink]] to publish a message to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def sink(eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequestEntry, CompletionStage[Done]] =
    flow(EventBridgePublishSettings(), eventBridgeClient)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Creates a [[pekko.stream.javadsl.Sink Sink]] to publish a message to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def sink(settings: EventBridgePublishSettings,
      eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequestEntry, CompletionStage[Done]] =
    flow(settings, eventBridgeClient)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Creates a [[pekko.stream.javadsl.Sink Sink]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishSink(settings: EventBridgePublishSettings,
      eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequest, CompletionStage[Done]] =
    publishFlow(settings, eventBridgeClient)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Creates a [[pekko.stream.javadsl.Sink Sink]] to publish messages to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def publishSink(eventBridgeClient: EventBridgeAsyncClient): Sink[PutEventsRequest, CompletionStage[Done]] =
    publishFlow(EventBridgePublishSettings(), eventBridgeClient)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
}
