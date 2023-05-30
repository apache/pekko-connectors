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

package org.apache.pekko.stream.connectors.google.firebase.fcm.impl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.http.scaladsl.Http
import pekko.stream.connectors.google.auth.{ Credentials, ServiceAccountCredentials }
import pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import pekko.stream.{ Attributes, Materializer }
import pekko.stream.connectors.google.firebase.fcm._
import pekko.stream.scaladsl.Flow

import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[fcm] object FcmFlows {

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows */
  @deprecated("org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows", "Alpakka 3.0.2")
  @Deprecated
  private[fcm] def fcmWithData[T](conf: FcmSettings): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        implicit val settings: GoogleSettings = resolveSettings(conf)(mat, attr)
        val sender = new FcmSender()
        Flow[(FcmNotification, T)].mapAsync(conf.maxConcurrentConnections) {
          case (notification, data) =>
            sender
              .send(Http(mat.system), FcmSend(conf.isTest, notification))(mat, settings)
              .zip(Future.successful(data))
        }
      }
      .mapMaterializedValue(_ => NotUsed)

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows */
  @deprecated("org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows", "Alpakka 3.0.2")
  @Deprecated
  private[fcm] def fcm(conf: FcmSettings): Flow[FcmNotification, FcmResponse, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        implicit val settings: GoogleSettings = resolveSettings(conf)(mat, attr)
        val sender = new FcmSender()
        Flow[FcmNotification].mapAsync(conf.maxConcurrentConnections) { notification =>
          sender.send(Http(mat.system), FcmSend(conf.isTest, notification))(mat, settings)
        }
      }
      .mapMaterializedValue(_ => NotUsed)

  @deprecated("org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows", "Alpakka 3.0.2")
  @Deprecated
  private def resolveSettings(conf: FcmSettings)(mat: Materializer, attr: Attributes): GoogleSettings = {
    val settings = GoogleAttributes.resolveSettings(mat, attr)
    val scopes = List("https://www.googleapis.com/auth/firebase.messaging")
    val credentials =
      if (conf.privateKey == "deprecated")
        settings.credentials
      else
        Credentials.cache((conf.projectId, conf.clientEmail, conf.privateKey, scopes, mat.system.name)) {
          ServiceAccountCredentials(
            conf.projectId,
            conf.clientEmail,
            conf.privateKey,
            scopes)(mat.system)
        }
    val forwardProxy =
      conf.forwardProxy.map(_.toCommonForwardProxy(mat.system)).orElse(settings.requestSettings.forwardProxy)
    settings.copy(
      credentials = credentials,
      requestSettings = settings.requestSettings.copy(forwardProxy = forwardProxy))
  }
}
