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

package org.apache.pekko.stream.connectors.amqp.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.dispatch.ExecutionContexts
import pekko.stream.connectors.amqp.impl
import pekko.stream.connectors.amqp.{ AmqpSourceSettings, ReadResult }
import pekko.stream.scaladsl.Source

import scala.concurrent.ExecutionContext

object AmqpSource {
  private implicit val executionContext: ExecutionContext = ExecutionContexts.parasitic

  /**
   * Scala API: Convenience for "at-most once delivery" semantics. Each message is acked to RabbitMQ
   * before it is emitted downstream.
   */
  def atMostOnceSource(settings: AmqpSourceSettings, bufferSize: Int): Source[ReadResult, NotUsed] =
    committableSource(settings, bufferSize)
      .mapAsync(1)(cm => cm.ack().map(_ => cm.message))

  /**
   * Scala API:
   * The `committableSource` makes it possible to commit (ack/nack) messages to RabbitMQ.
   * This is useful when "at-least once delivery" is desired, as each message will likely be
   * delivered one time but in failure cases could be duplicated.
   *
   * If you commit the offset before processing the message you get "at-most once delivery" semantics,
   * and for that there is a [[#atMostOnceSource]].
   *
   * Compared to auto-commit, this gives exact control over when a message is considered consumed.
   */
  def committableSource(settings: AmqpSourceSettings, bufferSize: Int): Source[CommittableReadResult, NotUsed] =
    Source.fromGraph(new impl.AmqpSourceStage(settings, bufferSize))

}
