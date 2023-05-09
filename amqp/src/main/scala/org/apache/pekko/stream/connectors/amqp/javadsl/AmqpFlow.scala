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

package org.apache.pekko.stream.connectors.amqp.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.japi.Pair
import pekko.stream.connectors.amqp._
import pekko.stream.scaladsl.Keep
import pekko.util.FutureConverters._

object AmqpFlow {

  /**
   * Creates an `AmqpFlow` that accepts `WriteMessage` elements and emits `WriteResult`.
   *
   * This variant of `AmqpFlow` publishes messages in a fire-and-forget manner, hence all emitted `WriteResult`s
   * have `confirmed` flag set to `true`.
   *
   * This stage materializes to a `CompletionStage` of `Done`, which can be used to know when the Flow completes,
   * either normally or because of an amqp failure.
   *
   * @param settings `bufferSize` and `confirmationTimeout` properties are ignored by this connector
   */
  def create(
      settings: AmqpWriteSettings): pekko.stream.javadsl.Flow[WriteMessage, WriteResult, CompletionStage[Done]] =
    pekko.stream.connectors.amqp.scaladsl.AmqpFlow(settings).mapMaterializedValue(f => f.asJava).asJava

  /**
   * Creates an `AmqpFlow` that accepts `WriteMessage` elements and emits `WriteResult`.
   *
   * This variant of `AmqpFlow` asynchronously waits for message confirmations. Maximum number of messages
   * simultaneously waiting for confirmation before signaling backpressure is configured with a
   * `bufferSize` parameter. Emitted results preserve the order of messages pulled from upstream - due to that
   * restriction this flow is expected to be slightly less effective than it's unordered counterpart.
   *
   * In case of upstream failure/finish this stage attempts to process all buffered messages (waiting for
   * confirmation) before propagating failure/finish downstream.
   *
   * This stage materializes to a `CompletionStage` of `Done`, which can be used to know when the Flow completes,
   * either normally or because of an amqp failure.
   *
   * NOTE: This connector uses RabbitMQ's extension to AMQP protocol
   * ([[https://www.rabbitmq.com/confirms.html#publisher-confirms Publisher Confirms]]), therefore it is not
   * supposed to be used with another AMQP brokers.
   */
  def createWithConfirm(
      settings: AmqpWriteSettings): pekko.stream.javadsl.Flow[WriteMessage, WriteResult, CompletionStage[Done]] =
    pekko.stream.connectors.amqp.scaladsl.AmqpFlow
      .withConfirm(settings = settings)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Creates an `AmqpFlow` that accepts `WriteMessage` elements and emits `WriteResult`.
   *
   * This variant of `AmqpFlow` asynchronously waits for message confirmations. Maximum number of messages
   * simultaneously waiting for confirmation before signaling backpressure is configured with a
   * `bufferSize` parameter. Results are emitted downstream as soon as confirmation is received, meaning that
   * there is no ordering guarantee of any sort.
   *
   * In case of upstream failure/finish this stage attempts to process all buffered messages (waiting for
   * confirmation) before propagating failure/finish downstream.
   *
   * This stage materializes to a `CompletionStage` of `Done`, which can be used to know when the Flow completes,
   * either normally or because of an amqp failure.
   *
   * NOTE: This connector uses RabbitMQ's extension to AMQP protocol
   * ([[https://www.rabbitmq.com/confirms.html#publisher-confirms Publisher Confirms]]), therefore it is not
   * supposed to be used with another AMQP brokers.
   */
  def createWithConfirmUnordered(
      settings: AmqpWriteSettings): pekko.stream.javadsl.Flow[WriteMessage, WriteResult, CompletionStage[Done]] =
    pekko.stream.connectors.amqp.scaladsl.AmqpFlow
      .withConfirmUnordered(settings)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Variant of `AmqpFlow.createWithConfirmUnordered` with additional support for pass-through elements.
   *
   * @see [[AmqpFlow.createWithConfirmUnordered]]
   *
   * NOTE: This connector uses RabbitMQ's extension to AMQP protocol
   * ([[https://www.rabbitmq.com/confirms.html#publisher-confirms Publisher Confirms]]), therefore it is not
   * supposed to be used with another AMQP brokers.
   */
  def createWithConfirmAndPassThroughUnordered[T](
      settings: AmqpWriteSettings)
      : pekko.stream.javadsl.Flow[Pair[WriteMessage, T], Pair[WriteResult, T], CompletionStage[Done]] =
    pekko.stream.scaladsl
      .Flow[Pair[WriteMessage, T]]
      .map((p: Pair[WriteMessage, T]) => p.toScala)
      .viaMat(
        pekko.stream.connectors.amqp.scaladsl.AmqpFlow
          .withConfirmAndPassThroughUnordered[T](settings = settings))(Keep.right)
      .map { case (writeResult, passThrough) => Pair(writeResult, passThrough) }
      .mapMaterializedValue(_.asJava)
      .asJava
}
