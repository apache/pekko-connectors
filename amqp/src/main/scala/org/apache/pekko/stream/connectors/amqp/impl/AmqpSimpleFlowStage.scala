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
import pekko.stream.{ ActorAttributes, Attributes, FlowShape, Inlet, Outlet }
import pekko.stream.connectors.amqp.{ AmqpWriteSettings, WriteMessage, WriteResult }
import pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue }

import scala.concurrent.{ Future, Promise }

/**
 * Internal API.
 *
 * Simplest AMQP flow. Published messages without any delivery guarantees. Result is emitted as soon as message is
 * published. Because there it's not verified whether message is confirmed or rejected, results are emitted always
 * as confirmed. Since result is always confirmed, it would also make sense to emit just plain `passThrough` object
 * instead of complete [[WriteResult]] (possibly it would be less confusing for users), but [[WriteResult]] is used
 * for consistency with other variants and to make the flow ready for any possible future [[WriteResult]] extensions.
 */
@InternalApi private[amqp] final class AmqpSimpleFlowStage[T](writeSettings: AmqpWriteSettings)
    extends GraphStageWithMaterializedValue[FlowShape[(WriteMessage, T), (WriteResult, T)], Future[Done]] { stage =>

  private val in: Inlet[(WriteMessage, T)] = Inlet(Logging.simpleName(this) + ".in")
  private val out: Outlet[(WriteResult, T)] = Outlet(Logging.simpleName(this) + ".out")

  override val shape: FlowShape[(WriteMessage, T), (WriteResult, T)] = FlowShape.of(in, out)

  override protected def initialAttributes: Attributes =
    Attributes.name(Logging.simpleName(this)) and ActorAttributes.IODispatcher

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val streamCompletion = Promise[Done]()
    (new AbstractAmqpFlowStageLogic[T](writeSettings, streamCompletion, shape) {
        override def publish(message: WriteMessage, passThrough: T): Unit = {
          log.debug("Publishing message {}.", message)

          channel.basicPublish(
            settings.exchange.getOrElse(""),
            message.routingKey.orElse(settings.routingKey).getOrElse(""),
            message.mandatory,
            message.immediate,
            message.properties.orNull,
            message.bytes.toArray)
          push(out, (WriteResult.confirmed, passThrough))
        }
      }, streamCompletion.future)
  }
}
