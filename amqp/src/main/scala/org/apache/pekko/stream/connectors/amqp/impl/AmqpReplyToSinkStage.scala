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

import org.apache.pekko
import pekko.Done
import pekko.annotation.InternalApi
import pekko.stream.connectors.amqp.{ AmqpReplyToSinkSettings, WriteMessage }
import pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, InHandler }
import pekko.stream.{ ActorAttributes, Attributes, Inlet, SinkShape }

import scala.concurrent.{ Future, Promise }

/**
 * Connects to an AMQP server upon materialization and sends write messages to the server.
 * Each materialized sink will create one connection to the broker. This stage sends messages to
 * the queue named in the replyTo options of the message instead of from settings declared at construction.
 */
@InternalApi
private[amqp] final class AmqpReplyToSinkStage(settings: AmqpReplyToSinkSettings)
    extends GraphStageWithMaterializedValue[SinkShape[WriteMessage], Future[Done]] { stage =>

  val in = Inlet[WriteMessage]("AmqpReplyToSink.in")

  override def shape: SinkShape[WriteMessage] = SinkShape.of(in)

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name("AmqpReplyToSink") and ActorAttributes.IODispatcher

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val streamCompletion = Promise[Done]()
    (new GraphStageLogic(shape) with AmqpConnectorLogic {
        override val settings = stage.settings

        override def whenConnected(): Unit = pull(in)

        override def postStop(): Unit = {
          streamCompletion.tryFailure(new RuntimeException("stage stopped unexpectedly"))
          super.postStop()
        }

        override def onFailure(ex: Throwable): Unit = {
          streamCompletion.tryFailure(ex)
          super.onFailure(ex)
        }

        setHandler(
          in,
          new InHandler {

            override def onUpstreamFailure(ex: Throwable): Unit = {
              streamCompletion.failure(ex)
              super.onUpstreamFailure(ex)
            }

            override def onUpstreamFinish(): Unit = {
              streamCompletion.success(Done)
              super.onUpstreamFinish()
            }

            override def onPush(): Unit = {
              val elem = grab(in)

              val replyTo = elem.properties.flatMap(properties => Option(properties.getReplyTo))

              if (replyTo.isDefined) {
                channel.basicPublish(
                  elem.routingKey.getOrElse(""),
                  replyTo.get,
                  elem.mandatory,
                  elem.immediate,
                  elem.properties.orNull,
                  elem.bytes.toArray)
              } else if (settings.failIfReplyToMissing) {
                onFailure(new RuntimeException("Reply-to header was not set"))
              }

              tryPull(in)
            }
          })

      }, streamCompletion.future)
  }

  override def toString: String = "AmqpReplyToSink"
}
