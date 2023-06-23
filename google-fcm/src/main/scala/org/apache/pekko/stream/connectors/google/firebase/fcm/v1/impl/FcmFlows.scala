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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.http.scaladsl.Http
import pekko.stream.connectors.google.firebase.fcm.FcmSettings
import pekko.stream.connectors.google.firebase.fcm.v1.models.{ FcmNotification, FcmResponse }
import pekko.stream.scaladsl.Flow
import pekko.stream.connectors.google.GoogleAttributes

import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[fcm] object FcmFlows {

  private[fcm] def fcmWithData[T](conf: FcmSettings): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        val settings = GoogleAttributes.resolveSettings(mat, attr)
        val sender = new FcmSender()
        Flow[(FcmNotification, T)].mapAsync(conf.maxConcurrentConnections) {
          case (notification, data) =>
            sender
              .send(Http(mat.system), FcmSend(conf.isTest, notification))(mat, settings)
              .zip(Future.successful(data))
        }
      }
      .mapMaterializedValue(_ => NotUsed)

  private[fcm] def fcm(conf: FcmSettings): Flow[FcmNotification, FcmResponse, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        val settings = GoogleAttributes.resolveSettings(mat, attr)
        val sender = new FcmSender()
        Flow[FcmNotification].mapAsync(conf.maxConcurrentConnections) { notification =>
          sender.send(Http(mat.system), FcmSend(conf.isTest, notification))(mat, settings)
        }
      }
      .mapMaterializedValue(_ => NotUsed)
}
