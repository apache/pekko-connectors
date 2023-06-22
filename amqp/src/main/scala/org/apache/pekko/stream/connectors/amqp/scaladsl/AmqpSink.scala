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

package org.apache.pekko.stream.connectors.amqp.scaladsl

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.amqp._
import pekko.stream.scaladsl.{ Keep, Sink }
import pekko.util.ByteString

import scala.concurrent.Future

object AmqpSink {

  /**
   * Creates an `AmqpSink` that accepts `WriteMessage` elements.
   *
   * This stage materializes to a `Future` of `Done`, which can be used to know when the Sink completes,
   * either normally or because of an amqp failure.
   */
  def apply(settings: AmqpWriteSettings): Sink[WriteMessage, Future[Done]] =
    AmqpFlow.apply(settings).toMat(Sink.ignore)(Keep.right)

  /**
   * Creates an `AmqpSink` that accepts `ByteString` elements.
   *
   * This stage materializes to a `Future` of `Done`, which can be used to know when the Sink completes,
   * either normally or because of an amqp failure.
   */
  def simple(settings: AmqpWriteSettings): Sink[ByteString, Future[Done]] =
    apply(settings).contramap[ByteString](bytes => WriteMessage(bytes))

  /**
   * Connects to an AMQP server upon materialization and sends incoming messages to the server.
   * Each materialized sink will create one connection to the broker. This stage sends messages to
   * the queue named in the replyTo options of the message instead of from settings declared at construction.
   *
   * This stage materializes to a `Future` of `Done`, which can be used to know when the Sink completes,
   * either normally or because of an amqp failure.
   */
  def replyTo(settings: AmqpReplyToSinkSettings): Sink[WriteMessage, Future[Done]] =
    Sink.fromGraph(new impl.AmqpReplyToSinkStage(settings))

}
