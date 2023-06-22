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

package org.apache.pekko.stream.connectors.udp.scaladsl

import java.net.InetSocketAddress

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.io.Inet.SocketOption
import pekko.stream.connectors.udp.Datagram
import pekko.stream.connectors.udp.impl.{ UdpBindFlow, UdpSendFlow }
import pekko.stream.scaladsl.Sink
import pekko.stream.scaladsl.Flow

import scala.collection.immutable.Iterable
import scala.concurrent.Future

object Udp {

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param system implicit actor system
   */
  def sendFlow()(implicit system: ClassicActorSystemProvider): Flow[Datagram, Datagram, NotUsed] =
    sendFlow(system.classicSystem)

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param system the actor system
   */
  def sendFlow(system: ActorSystem): Flow[Datagram, Datagram, NotUsed] =
    Flow.fromGraph(new UdpSendFlow()(system))

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param options UDP socket options
   * @param system implicit actor system
   */
  def sendFlow(
      options: Iterable[SocketOption])(implicit system: ClassicActorSystemProvider): Flow[Datagram, Datagram, NotUsed] =
    sendFlow(options, system.classicSystem)

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param options UDP socket options
   * @param system the actor system
   */
  def sendFlow(options: Iterable[SocketOption], system: ActorSystem): Flow[Datagram, Datagram, NotUsed] =
    Flow.fromGraph(new UdpSendFlow(options)(system))

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param system implicit actor system
   */
  def sendSink()(implicit system: ClassicActorSystemProvider): Sink[Datagram, NotUsed] = sendFlow().to(Sink.ignore)

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param system the actor system
   */
  def sendSink(system: ActorSystem): Sink[Datagram, NotUsed] = sendFlow(system).to(Sink.ignore)

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param options UDP socket options
   * @param system implicit actor system
   */
  def sendSink(options: Iterable[SocketOption])(
      implicit system: ClassicActorSystemProvider): Sink[Datagram, NotUsed] = sendFlow(options).to(Sink.ignore)

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param options UDP socket options
   * @param system the actor system
   */
  def sendSink(options: Iterable[SocketOption], system: ActorSystem): Sink[Datagram, NotUsed] =
    sendFlow(options, system).to(Sink.ignore)

  /**
   * Creates a flow that upon materialization binds to the given `localAddress`. All incoming
   * messages to the `localAddress` are emitted from the flow. All incoming messages to the flow
   * are sent to the remote address contained in the message.
   *
   * @param localAddress UDP socket address
   * @param system implicit actor system
   */
  def bindFlow(
      localAddress: InetSocketAddress)(
      implicit system: ClassicActorSystemProvider): Flow[Datagram, Datagram, Future[InetSocketAddress]] =
    bindFlow(localAddress, system.classicSystem)

  /**
   * Creates a flow that upon materialization binds to the given `localAddress`. All incoming
   * messages to the `localAddress` are emitted from the flow. All incoming messages to the flow
   * are sent to the remote address contained in the message.
   *
   * @param localAddress UDP socket address
   * @param system the actor system
   */
  def bindFlow(localAddress: InetSocketAddress,
      system: ActorSystem): Flow[Datagram, Datagram, Future[InetSocketAddress]] =
    Flow.fromGraph(new UdpBindFlow(localAddress)(system))

  /**
   * Creates a flow that upon materialization binds to the given `localAddress`. All incoming
   * messages to the `localAddress` are emitted from the flow. All incoming messages to the flow
   * are sent to the remote address contained in the message.
   *
   * @param localAddress UDP socket address
   * @param options UDP socket options
   * @param system implicit actor system
   */
  def bindFlow(
      localAddress: InetSocketAddress,
      options: Iterable[SocketOption])(
      implicit system: ClassicActorSystemProvider): Flow[Datagram, Datagram, Future[InetSocketAddress]] =
    bindFlow(localAddress, options, system.classicSystem)

  /**
   * Creates a flow that upon materialization binds to the given `localAddress`. All incoming
   * messages to the `localAddress` are emitted from the flow. All incoming messages to the flow
   * are sent to the remote address contained in the message.
   *
   * @param localAddress UDP socket address
   * @param options UDP socket options
   * @param system the actor system
   */
  def bindFlow(localAddress: InetSocketAddress,
      options: Iterable[SocketOption],
      system: ActorSystem): Flow[Datagram, Datagram, Future[InetSocketAddress]] =
    Flow.fromGraph(new UdpBindFlow(localAddress, options)(system))
}
