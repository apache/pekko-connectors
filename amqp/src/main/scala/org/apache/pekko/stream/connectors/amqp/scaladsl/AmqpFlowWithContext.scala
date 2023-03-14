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

import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.amqp._
import org.apache.pekko.stream.scaladsl.{ Flow, FlowWithContext }

import scala.concurrent.Future

object AmqpFlowWithContext {

  /**
   * Creates a contextual variant of corresponding [[AmqpFlow]].
   *
   * @see [[AmqpFlow.apply]]
   */
  def apply[T](
      settings: AmqpWriteSettings): FlowWithContext[WriteMessage, T, WriteResult, T, Future[Done]] =
    FlowWithContext.fromTuples(
      Flow.fromGraph(new impl.AmqpSimpleFlowStage[T](settings)))

  /**
   * Creates a contextual variant of corresponding [[AmqpFlow]].
   *
   * @see [[AmqpFlow.withConfirm]]
   *
   * NOTE: This connector uses RabbitMQ's extension to AMQP protocol
   * ([[https://www.rabbitmq.com/confirms.html#publisher-confirms Publisher Confirms]]), therefore it is not
   * supposed to be used with another AMQP brokers.
   */
  def withConfirm[T](
      settings: AmqpWriteSettings): FlowWithContext[WriteMessage, T, WriteResult, T, Future[Done]] =
    FlowWithContext.fromTuples(
      Flow.fromGraph(new impl.AmqpAsyncFlowStage(settings)))
}
