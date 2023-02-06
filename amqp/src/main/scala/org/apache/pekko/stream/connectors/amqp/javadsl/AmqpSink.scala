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

import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.amqp._
import org.apache.pekko.util.ByteString

import scala.compat.java8.FutureConverters._

object AmqpSink {

  /**
   * Creates an `AmqpSink` that accepts `WriteMessage` elements.
   *
   * This stage materializes to a `CompletionStage` of `Done`, which can be used to know when the Sink completes,
   * either normally or because of an amqp failure.
   */
  def create(settings: AmqpWriteSettings): org.apache.pekko.stream.javadsl.Sink[WriteMessage, CompletionStage[Done]] =
    org.apache.pekko.stream.connectors.amqp.scaladsl.AmqpSink(settings).mapMaterializedValue(f => f.toJava).asJava

  /**
   * Creates an `AmqpSink` that accepts `ByteString` elements.
   *
   * This stage materializes to a `CompletionStage` of `Done`, which can be used to know when the Sink completes,
   * either normally or because of an amqp failure.
   */
  def createSimple(
      settings: AmqpWriteSettings): org.apache.pekko.stream.javadsl.Sink[ByteString, CompletionStage[Done]] =
    org.apache.pekko.stream.connectors.amqp.scaladsl.AmqpSink.simple(settings).mapMaterializedValue(f =>
      f.toJava).asJava

  /**
   * Connects to an AMQP server upon materialization and sends incoming messages to the server.
   * Each materialized sink will create one connection to the broker. This stage sends messages to
   * the queue named in the replyTo options of the message instead of from settings declared at construction.
   *
   * This stage materializes to a `CompletionStage` of `Done`, which can be used to know when the Sink completes,
   * either normally or because of an amqp failure.
   */
  def createReplyTo(
      settings: AmqpReplyToSinkSettings): org.apache.pekko.stream.javadsl.Sink[WriteMessage, CompletionStage[Done]] =
    org.apache.pekko.stream.connectors.amqp.scaladsl.AmqpSink.replyTo(settings).mapMaterializedValue(f =>
      f.toJava).asJava

}
