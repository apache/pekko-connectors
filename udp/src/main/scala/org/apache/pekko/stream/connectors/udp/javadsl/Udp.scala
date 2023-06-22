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

package org.apache.pekko.stream.connectors.udp.javadsl

import java.net.InetSocketAddress
import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.NotUsed
import pekko.io.Inet.SocketOption
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.stream.connectors.udp.Datagram
import pekko.stream.javadsl.{ Flow, Sink }
import pekko.stream.connectors.udp.scaladsl
import pekko.util.ccompat.JavaConverters._
import pekko.util.FutureConverters._

object Udp {
  import java.lang.{ Iterable => JIterable }

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param system the actor system
   */
  def sendFlow(system: ActorSystem): Flow[Datagram, Datagram, NotUsed] =
    scaladsl.Udp.sendFlow()(system).asJava

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param system the actor system
   */
  def sendFlow(system: ClassicActorSystemProvider): Flow[Datagram, Datagram, NotUsed] =
    scaladsl.Udp.sendFlow()(system).asJava

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param options UDP socket options
   * @param system the actor system
   */
  def sendFlow(options: JIterable[SocketOption], system: ActorSystem): Flow[Datagram, Datagram, NotUsed] =
    scaladsl.Udp.sendFlow(options.asScala.toIndexedSeq)(system).asJava

  /**
   * Creates a flow that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message. All incoming messages are also emitted from the flow for
   * subsequent processing.
   *
   * @param options UDP socket options
   * @param system the actor system
   */
  def sendFlow(options: JIterable[SocketOption],
      system: ClassicActorSystemProvider): Flow[Datagram, Datagram, NotUsed] =
    scaladsl.Udp.sendFlow(options.asScala.toIndexedSeq)(system).asJava

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param system the actor system
   */
  def sendSink(system: ActorSystem): Sink[Datagram, NotUsed] =
    scaladsl.Udp.sendSink()(system).asJava

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param system the actor system
   */
  def sendSink(system: ClassicActorSystemProvider): Sink[Datagram, NotUsed] =
    scaladsl.Udp.sendSink()(system).asJava

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param options UDP socket options
   * @param system the actor system
   */
  def sendSink(options: JIterable[SocketOption], system: ActorSystem): Sink[Datagram, NotUsed] =
    scaladsl.Udp.sendSink(options.asScala.toIndexedSeq)(system).asJava

  /**
   * Creates a sink that will send all incoming [UdpMessage] messages to the remote address
   * contained in the message.
   *
   * @param options UDP socket options
   * @param system the actor system
   */
  def sendSink(options: JIterable[SocketOption], system: ClassicActorSystemProvider): Sink[Datagram, NotUsed] =
    scaladsl.Udp.sendSink(options.asScala.toIndexedSeq)(system).asJava

  /**
   * Creates a flow that upon materialization binds to the given `localAddress`. All incoming
   * messages to the `localAddress` are emitted from the flow. All incoming messages to the flow
   * are sent to the remote address contained in the message.
   *
   * @param localAddress UDP socket address
   * @param system the actor system
   */
  def bindFlow(localAddress: InetSocketAddress,
      system: ActorSystem): Flow[Datagram, Datagram, CompletionStage[InetSocketAddress]] =
    scaladsl.Udp.bindFlow(localAddress)(system).mapMaterializedValue(_.asJava).asJava

  /**
   * Creates a flow that upon materialization binds to the given `localAddress`. All incoming
   * messages to the `localAddress` are emitted from the flow. All incoming messages to the flow
   * are sent to the remote address contained in the message.
   *
   * @param localAddress UDP socket address
   * @param system the actor system
   */
  def bindFlow(localAddress: InetSocketAddress,
      system: ClassicActorSystemProvider): Flow[Datagram, Datagram, CompletionStage[InetSocketAddress]] =
    scaladsl.Udp.bindFlow(localAddress)(system).mapMaterializedValue(_.asJava).asJava

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
      options: JIterable[SocketOption],
      system: ActorSystem): Flow[Datagram, Datagram, CompletionStage[InetSocketAddress]] =
    scaladsl.Udp.bindFlow(localAddress, options.asScala.toIndexedSeq)(system).mapMaterializedValue(_.asJava).asJava

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
      options: JIterable[SocketOption],
      system: ClassicActorSystemProvider): Flow[Datagram, Datagram, CompletionStage[InetSocketAddress]] =
    scaladsl.Udp.bindFlow(localAddress, options.asScala.toIndexedSeq)(system).mapMaterializedValue(_.asJava).asJava
}
