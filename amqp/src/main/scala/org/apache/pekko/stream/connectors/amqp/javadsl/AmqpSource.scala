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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.amqp.{ AmqpSourceSettings, ReadResult }
import org.apache.pekko.stream.javadsl.Source

object AmqpSource {

  /**
   * Java API: Convenience for "at-most once delivery" semantics. Each message is acked to RabbitMQ
   * before it is emitted downstream.
   */
  def atMostOnceSource(settings: AmqpSourceSettings, bufferSize: Int): Source[ReadResult, NotUsed] =
    org.apache.pekko.stream.connectors.amqp.scaladsl.AmqpSource
      .atMostOnceSource(settings, bufferSize)
      .asJava

  /**
   * Java API:
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
    org.apache.pekko.stream.connectors.amqp.scaladsl.AmqpSource
      .committableSource(settings, bufferSize)
      .map(cm => new CommittableReadResult(cm))
      .asJava

}
