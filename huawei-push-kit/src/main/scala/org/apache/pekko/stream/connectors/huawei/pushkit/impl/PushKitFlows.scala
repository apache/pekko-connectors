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

package org.apache.pekko.stream.connectors.huawei.pushkit.impl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.http.scaladsl.Http
import pekko.stream.connectors.huawei.pushkit.HmsSettings
import pekko.stream.connectors.huawei.pushkit.models.{ PushKitNotification, Response }
import pekko.stream.scaladsl.Flow

/**
 * INTERNAL API
 */
@InternalApi
private[pushkit] object PushKitFlows {

  private[pushkit] def pushKit(conf: HmsSettings): Flow[PushKitNotification, Response, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        import materializer.executionContext
        val http = Http()(materializer.system)
        val session: HmsSession =
          new HmsSession(conf, new HmsTokenApi(http, materializer.system, conf.forwardProxy))
        val sender: PushKitSender = new PushKitSender()
        Flow[PushKitNotification]
          .mapAsync(conf.maxConcurrentConnections)(in =>
            session.getToken()(materializer).flatMap { token =>
              sender.send(conf, token, http, PushKitSend(conf.test, in), materializer.system)(materializer)
            })
      }
      .mapMaterializedValue(_ => NotUsed)
}
