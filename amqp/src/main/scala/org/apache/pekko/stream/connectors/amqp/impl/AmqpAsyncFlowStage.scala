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

package org.apache.pekko.stream.connectors.amqp.impl

import org.apache.pekko
import pekko.Done
import pekko.annotation.InternalApi
import pekko.event.Logging
import pekko.stream.connectors.amqp.impl.AbstractAmqpAsyncFlowStageLogic.DeliveryTag
import pekko.stream.connectors.amqp.{ AmqpWriteSettings, WriteMessage, WriteResult }
import pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue }
import pekko.stream._

import scala.collection.immutable.TreeMap
import scala.concurrent.{ Future, Promise }

/**
 * Internal API.
 *
 * AMQP flow that emits confirmation results in input order.
 *
 * Messages stay in an ordered buffer until they have a result and all earlier messages can be emitted.
 * Each message keeps its first result, whether it comes from a confirmation, rejection or timeout.
 * With `multiple` set, a broker confirmation or rejection applies to all delivery tags up to and including
 * the supplied tag. Results already recorded are preserved.
 */
@InternalApi private[amqp] final class AmqpAsyncFlowStage[T](
    settings: AmqpWriteSettings)
    extends GraphStageWithMaterializedValue[FlowShape[(WriteMessage, T), (WriteResult, T)], Future[Done]] {

  val in: Inlet[(WriteMessage, T)] = Inlet(Logging.simpleName(this) + ".in")
  val out: Outlet[(WriteResult, T)] = Outlet(Logging.simpleName(this) + ".out")

  override def shape: FlowShape[(WriteMessage, T), (WriteResult, T)] = FlowShape.of(in, out)

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name(Logging.simpleName(this)) and ActorAttributes.IODispatcher

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val streamCompletion = Promise[Done]()
    (new AbstractAmqpAsyncFlowStageLogic(settings, streamCompletion, shape) {

        private var buffer = TreeMap[DeliveryTag, AwaitingMessage[T]]()

        override def enqueueMessage(tag: DeliveryTag, passThrough: T): Unit =
          buffer += (tag -> AwaitingMessage(tag, passThrough))

        override def dequeueAwaitingMessages(
            tag: DeliveryTag, multiple: Boolean, result: WriteResult): Iterable[AwaitingMessage[T]] = {
          val affectedMessages = if (multiple) buffer.rangeTo(tag).values else buffer.get(tag).toList
          affectedMessages.foreach { message =>
            if (message.result.isEmpty) buffer += (message.tag -> message.copy(result = Some(result)))
          }

          val dequeued = buffer.takeWhile { case (_, message) => message.result.isDefined }
          buffer --= dequeued.keys
          dequeued.values
        }

        override def messagesAwaitingDelivery: Int = buffer.size

        override def noAwaitingMessages: Boolean = buffer.isEmpty

      }, streamCompletion.future)
  }
}
