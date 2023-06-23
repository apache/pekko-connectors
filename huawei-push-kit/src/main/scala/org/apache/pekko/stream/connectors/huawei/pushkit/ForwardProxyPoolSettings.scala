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

package org.apache.pekko.stream.connectors.huawei.pushkit

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.annotation.InternalApi
import pekko.http.scaladsl.ClientTransport
import pekko.http.scaladsl.model.headers.BasicHttpCredentials
import pekko.http.scaladsl.settings.{ ClientConnectionSettings, ConnectionPoolSettings }

import java.net.InetSocketAddress

/**
 * INTERNAL API
 */
@InternalApi
private[pushkit] object ForwardProxyPoolSettings {

  implicit class ForwardProxyPoolSettings(forwardProxy: ForwardProxy) {

    def poolSettings(system: ActorSystem) = {
      val address = InetSocketAddress.createUnresolved(forwardProxy.host, forwardProxy.port)
      val transport = forwardProxy.credentials.fold(ClientTransport.httpsProxy(address))(c =>
        ClientTransport.httpsProxy(address, BasicHttpCredentials(c.username, c.password)))

      ConnectionPoolSettings(system)
        .withConnectionSettings(
          ClientConnectionSettings(system)
            .withTransport(transport))
    }
  }

}
