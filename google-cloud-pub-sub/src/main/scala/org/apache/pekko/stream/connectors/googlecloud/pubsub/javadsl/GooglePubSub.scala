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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.javadsl

import org.apache.pekko
import pekko.actor.Cancellable
import pekko.stream.connectors.googlecloud.pubsub.scaladsl.{ GooglePubSub => GPubSub }
import pekko.stream.connectors.googlecloud.pubsub.{ AcknowledgeRequest, PubSubConfig, PublishRequest, ReceivedMessage }
import pekko.stream.javadsl.{ Flow, FlowWithContext, Sink, Source }
import pekko.{ Done, NotUsed }
import pekko.util.ccompat.JavaConverters._
import pekko.util.FutureConverters._

import java.util.concurrent.CompletionStage
import scala.concurrent.Future

/**
 * Java DSL for Google Pub/Sub
 */
object GooglePubSub {

  /**
   * Creates a flow to that publishes messages to a topic and emits the message ids.
   * @param overrideHost publish message will be sent to specific host,
   *                     can be used to send message to specific regional endpoint,
   *                     which can be important when ordering is enabled
   */
  def publish(topic: String,
      config: PubSubConfig,
      overrideHost: String,
      parallelism: Int): Flow[PublishRequest, java.util.List[String], NotUsed] =
    GPubSub
      .publish(topic, config, overrideHost, parallelism)
      .map(response => response.asJava)
      .asJava

  /**
   * Creates a flow to that publishes messages to a topic and emits the message ids.
   */
  def publish(topic: String,
      config: PubSubConfig,
      parallelism: Int): Flow[PublishRequest, java.util.List[String], NotUsed] =
    GPubSub
      .publish(topic, config, parallelism)
      .map(response => response.asJava)
      .asJava

  /**
   * Creates a flow to that publishes messages to a topic and emits the message ids and carries a context
   * through.
   * @param overrideHost publish message will be sent to specific host,
   *                     can be used to send message to specific regional endpoint,
   *                     which can be important when ordering is enabled
   */
  def publishWithContext[C](topic: String,
      config: PubSubConfig,
      overrideHost: String,
      parallelism: Int): FlowWithContext[PublishRequest, C, java.util.List[String], C, NotUsed] =
    GPubSub
      .publishWithContext[C](topic, config, overrideHost, parallelism)
      .map(response => response.asJava)
      .asJava

  /**
   * Creates a flow to that publishes messages to a topic and emits the message ids and carries a context
   * through.
   */
  def publishWithContext[C](topic: String,
      config: PubSubConfig,
      parallelism: Int): FlowWithContext[PublishRequest, C, java.util.List[String], C, NotUsed] =
    GPubSub
      .publishWithContext[C](topic, config, parallelism)
      .map(response => response.asJava)
      .asJava

  /**
   * Creates a source pulling messages from a subscription.
   */
  def subscribe(subscription: String, config: PubSubConfig): Source[ReceivedMessage, Cancellable] =
    GPubSub
      .subscribe(subscription, config)
      .asJava

  /**
   * Creates a flow pulling messages from a subscription.
   */
  def subscribeFlow(subscription: String, config: PubSubConfig): Flow[Done, ReceivedMessage, Future[NotUsed]] =
    GPubSub
      .subscribeFlow(subscription, config)
      .asJava

  /**
   * Creates a flow for acknowledging messages on a subscription.
   */
  def acknowledgeFlow(subscription: String, config: PubSubConfig): Flow[AcknowledgeRequest, Done, NotUsed] =
    GPubSub
      .acknowledgeFlow(subscription, config)
      .asJava

  /**
   * Creates a sink for acknowledging messages on a subscription.
   */
  def acknowledge(subscription: String, config: PubSubConfig): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    GPubSub
      .acknowledge(subscription, config)
      .mapMaterializedValue(_.asJava)
      .asJava
}
