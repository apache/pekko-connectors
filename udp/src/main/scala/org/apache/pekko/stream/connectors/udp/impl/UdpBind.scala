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

package org.apache.pekko.stream.connectors.udp.impl

import java.net.InetSocketAddress

import org.apache.pekko
import pekko.actor.{ ActorRef, ActorSystem, Terminated }
import pekko.annotation.InternalApi
import pekko.io.{ IO, Udp }
import pekko.io.Inet.SocketOption
import pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import pekko.stream.connectors.udp.Datagram
import pekko.stream.stage._

import java.io.IOException
import scala.collection.immutable.Iterable
import scala.concurrent.{ Future, Promise }

/**
 * Binds to the given local address using UDP manager actor.
 */
@InternalApi private[udp] final class UdpBindLogic(localAddress: InetSocketAddress,
    options: Iterable[SocketOption],
    boundPromise: Promise[InetSocketAddress])(
    val shape: FlowShape[Datagram, Datagram])(implicit val system: ActorSystem)
    extends GraphStageLogic(shape) {

  private def in = shape.in
  private def out = shape.out

  private var listener: ActorRef = _

  override def preStart(): Unit = {
    implicit val sender: ActorRef = getStageActor(processIncoming).ref
    IO(Udp) ! Udp.Bind(sender, localAddress, options)
  }

  override def postStop(): Unit =
    unbindListener()

  private def processIncoming(event: (ActorRef, Any)): Unit = event match {
    case (sender, Udp.Bound(boundAddress)) =>
      boundPromise.success(boundAddress)
      listener = sender
      stageActor.watch(listener)
      pull(in)
    case (_, Udp.CommandFailed(cmd: Udp.Bind)) =>
      val ex = new IllegalArgumentException(s"Unable to bind to [${cmd.localAddress}]")
      boundPromise.failure(ex)
      failStage(ex)
    case (_, Udp.Received(data, sender)) =>
      if (isAvailable(out)) {
        push(out, Datagram(data, sender))
      }
    case (_, Terminated(_)) =>
      listener = null
      failStage(new IOException("UDP listener terminated unexpectedly"))
    case _ =>
  }

  private def unbindListener(): Unit =
    if (listener != null) {
      listener ! Udp.Unbind
    }

  setHandler(
    in,
    new InHandler {
      override def onPush(): Unit = {
        val msg = grab(in)
        listener ! Udp.Send(msg.data, msg.remote)
        pull(in)
      }
    })

  setHandler(
    out,
    new OutHandler {
      override def onPull(): Unit = ()
    })
}

@InternalApi private[udp] final class UdpBindFlow(localAddress: InetSocketAddress,
    options: Iterable[SocketOption] = Nil)(
    implicit val system: ActorSystem)
    extends GraphStageWithMaterializedValue[FlowShape[Datagram, Datagram], Future[InetSocketAddress]] {

  val in: Inlet[Datagram] = Inlet("UdpBindFlow.in")
  val out: Outlet[Datagram] = Outlet("UdpBindFlow.in")

  val shape: FlowShape[Datagram, Datagram] = FlowShape.of(in, out)
  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (UdpBindLogic, Future[InetSocketAddress]) = {
    val boundPromise = Promise[InetSocketAddress]()
    (new UdpBindLogic(localAddress, options, boundPromise)(shape), boundPromise.future)
  }
}
