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

package org.apache.pekko.stream.connectors.amqp.impl

import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.event.Logging
import org.apache.pekko.stream.connectors.amqp.impl.AbstractAmqpAsyncFlowStageLogic.DeliveryTag
import org.apache.pekko.stream.connectors.amqp.{ AmqpWriteSettings, WriteMessage, WriteResult }
import org.apache.pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue }
import org.apache.pekko.stream._

import scala.collection.mutable
import scala.concurrent.{ Future, Promise }

/**
 * Internal API.
 *
 * AMQP flow that uses asynchronous confirmations in possibly the most efficient way.
 * Messages are dequeued and pushed downstream as soon as confirmation is received. Flag `ready` on [[AwaitingMessage]]
 * is not used in this case. Flag `multiple` on a confirmation means that broker confirms all messages up to a
 * given delivery tag, which means that so all messages up to (and including) this delivery tag can be safely dequeued.
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

        override def dequeueAwaitingMessages(tag: DeliveryTag, multiple: Boolean): Iterable[AwaitingMessage[T]] =
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
