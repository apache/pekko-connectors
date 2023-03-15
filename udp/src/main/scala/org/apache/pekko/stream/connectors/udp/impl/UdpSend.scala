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

package org.apache.pekko.stream.connectors.udp.impl

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem, PoisonPill }
import pekko.annotation.InternalApi
import pekko.io.{ IO, Udp }
import pekko.io.Inet.SocketOption
import pekko.stream.connectors.udp.Datagram
import pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import pekko.stream._

import scala.collection.immutable.Iterable

/**
 * Sends incoming messages to the corresponding destination addresses.
 * After send command is issued to the UDP manager actor the message
 * is passed-through to the output for possible further processing.
 */
@InternalApi private[udp] final class UdpSendLogic(val shape: FlowShape[Datagram, Datagram],
    options: Iterable[SocketOption])(
    implicit val system: ActorSystem) extends GraphStageLogic(shape) {

  implicit def self: ActorRef = stageActor.ref

  private def in = shape.in
  private def out = shape.out

  private var simpleSender: ActorRef = _

  override def preStart(): Unit = {
    getStageActor(processIncoming)
    IO(Udp) ! Udp.SimpleSender(options)
  }

  override def postStop(): Unit =
    stopSimpleSender()

  private def processIncoming(event: (ActorRef, Any)): Unit = event match {
    case (sender, Udp.SimpleSenderReady) =>
      simpleSender = sender
      pull(in)
    case _ =>
  }

  private def stopSimpleSender() =
    if (simpleSender != null) {
      simpleSender ! PoisonPill
    }

  setHandler(
    in,
    new InHandler {
      override def onPush() = {
        val msg = grab(in)
        simpleSender ! Udp.Send(msg.data, msg.remote)
        push(out, msg)
      }
    })

  setHandler(
    out,
    new OutHandler {
      override def onPull(): Unit = if (simpleSender != null) pull(in)
    })
}

@InternalApi private[udp] final class UdpSendFlow(options: Iterable[SocketOption] = Nil)(
    implicit val system: ActorSystem) extends GraphStage[FlowShape[Datagram, Datagram]] {

  val in: Inlet[Datagram] = Inlet("UdpSendFlow.in")
  val out: Outlet[Datagram] = Outlet("UdpSendFlow.in")

  val shape: FlowShape[Datagram, Datagram] = FlowShape.of(in, out)
  override def createLogic(inheritedAttributes: Attributes) = new UdpSendLogic(shape, options)
}
