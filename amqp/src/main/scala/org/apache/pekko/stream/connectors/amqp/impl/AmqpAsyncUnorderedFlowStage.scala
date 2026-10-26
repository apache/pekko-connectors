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

import scala.collection.mutable
import scala.concurrent.{ Future, Promise }

/**
 * Internal API.
 *
 * AMQP flow that emits confirmation results without preserving input order.
 *
 * Messages are dequeued as soon as a confirmation, rejection or timeout arrives, so [[AwaitingMessage]] does not
 * need to store the result. With `multiple` set, all messages up to and including the supplied delivery tag
 * are dequeued.
 */
@InternalApi private[amqp] final class AmqpAsyncUnorderedFlowStage[T](
    settings: AmqpWriteSettings)
    extends GraphStageWithMaterializedValue[FlowShape[(WriteMessage, T), (WriteResult, T)], Future[Done]] {

  private val in: Inlet[(WriteMessage, T)] = Inlet(Logging.simpleName(this) + ".in")
  private val out: Outlet[(WriteResult, T)] = Outlet(Logging.simpleName(this) + ".out")

  override val shape: FlowShape[(WriteMessage, T), (WriteResult, T)] = FlowShape.of(in, out)

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name(Logging.simpleName(this)) and ActorAttributes.IODispatcher

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val streamCompletion = Promise[Done]()
    (new AbstractAmqpAsyncFlowStageLogic(settings, streamCompletion, shape) {

        private val buffer = mutable.Queue.empty[AwaitingMessage[T]]

        override def enqueueMessage(tag: DeliveryTag, passThrough: T): Unit =
          buffer += AwaitingMessage(tag, passThrough)

        override def dequeueAwaitingMessages(
            tag: DeliveryTag, multiple: Boolean, result: WriteResult): Iterable[AwaitingMessage[T]] =
          if (multiple)
            buffer.dequeueAll(_.tag <= tag)
          else
            buffer
              .dequeueFirst(_.tag == tag)
              .fold(Seq.empty[AwaitingMessage[T]])(Seq(_))

        override def messagesAwaitingDelivery: Int = buffer.length

        override def noAwaitingMessages: Boolean = buffer.isEmpty

      }, streamCompletion.future)
  }
}
