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
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages in batches to an EventBridge.
   * Each incoming list of entries is sent as a single `PutEventsRequest`.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flowList(settings: EventBridgePublishSettings, eventBridgeClient: EventBridgeAsyncClient)
      : Flow[java.util.List[PutEventsRequestEntry], PutEventsResponse, NotUsed] =
    pekko.stream.scaladsl
      .Flow[java.util.List[PutEventsRequestEntry]]
      .map(entries => PutEventsRequest.builder().entries(entries).build())
      .via(pekko.stream.connectors.aws.eventbridge.scaladsl.EventBridgePublisher.publishFlow(settings)(
        eventBridgeClient))
      .asJava

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages in batches to an EventBridge.
   * Each incoming list of entries is sent as a single `PutEventsRequest`.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   */
  def flowList(
      eventBridgeClient: EventBridgeAsyncClient)
      : Flow[java.util.List[PutEventsRequestEntry], PutEventsResponse, NotUsed] =
    flowList(EventBridgePublishSettings(), eventBridgeClient)

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param settings [[pekko.stream.connectors.aws.eventbridge.EventBridgePublishSettings]] settings for publishing
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   * @deprecated Despite its name this does not batch; it behaves exactly like `flow`. Use `flowList` to publish in batches, or `flow`.
   */
  @deprecated("Does not batch; use flowList to publish in batches, or flow", "2.0.0")
  def flowSeq(
      settings: EventBridgePublishSettings,
      eventBridgeClient: EventBridgeAsyncClient): Flow[PutEventsRequestEntry, PutEventsResponse, NotUsed] =
    flow(settings, eventBridgeClient)

  /**
   * Creates a [[pekko.stream.javadsl.Flow Flow]] to publish messages to an EventBridge.
   *
   * @param eventBridgeClient [[software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient EventBridgeAsyncClient]] client for publishing
   * @deprecated Takes a Scala `Seq`; use `flowList`.
   */
  @deprecated("Use flowList, which takes a java.util.List", "2.0.0")
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
