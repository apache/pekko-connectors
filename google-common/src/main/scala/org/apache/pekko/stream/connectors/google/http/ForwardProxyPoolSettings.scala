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

package org.apache.pekko.stream.connectors.google.http

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.annotation.InternalApi
import pekko.http.scaladsl.ClientTransport
import pekko.http.scaladsl.Http.OutgoingConnection
import pekko.http.scaladsl.model.headers.BasicHttpCredentials
import pekko.http.scaladsl.settings.{ ClientConnectionSettings, ConnectionPoolSettings }
import pekko.stream.scaladsl.{ Flow, Tcp }
import pekko.util.ByteString

import java.net.InetSocketAddress
import scala.concurrent.Future

@InternalApi
private[google] object ForwardProxyPoolSettings {

  def apply(scheme: String, host: String, port: Int, credentials: Option[BasicHttpCredentials])(
      implicit system: ActorSystem): ConnectionPoolSettings = {
    val address = InetSocketAddress.createUnresolved(host, port)
    val transport = scheme match {
      case "https" =>
        credentials.fold(ClientTransport.httpsProxy(address))(ClientTransport.httpsProxy(address, _))
      case "http" =>
        new ChangeTargetEndpointTransport(address)
      case _ =>
        throw new IllegalArgumentException("scheme must be either `http` or `https`")
    }
    ConnectionPoolSettings(system)
      .withConnectionSettings(
        ClientConnectionSettings(system)
          .withTransport(transport))
  }

}

private[http] final class ChangeTargetEndpointTransport(address: InetSocketAddress) extends ClientTransport {
  def connectTo(ignoredHost: String, ignoredPort: Int, settings: ClientConnectionSettings)(
      implicit system: ActorSystem): Flow[ByteString, ByteString, Future[OutgoingConnection]] =
    Tcp()
      .outgoingConnection(address,
        settings.localAddress,
        settings.socketOptions,
        halfClose = true,
        settings.connectingTimeout,
        settings.idleTimeout)
      .mapMaterializedValue(
        _.map(tcpConn => OutgoingConnection(tcpConn.localAddress, tcpConn.remoteAddress))(system.dispatcher))
}
