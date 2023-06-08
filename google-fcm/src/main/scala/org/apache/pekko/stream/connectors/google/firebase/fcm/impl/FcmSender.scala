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
import pekko.annotation.InternalApi
import pekko.http.scaladsl.HttpExt
import pekko.http.scaladsl.marshalling.Marshal
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.unmarshalling.{ FromResponseUnmarshaller, Unmarshal, Unmarshaller }
import pekko.stream.Materializer
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.google.firebase.fcm.{ FcmErrorResponse, FcmResponse, FcmSuccessResponse }
import pekko.stream.connectors.google.http.GoogleHttp
import pekko.stream.connectors.google.implicits._

import scala.annotation.nowarn
import scala.concurrent.Future

/**
 * INTERNAL API
 */
@nowarn("msg=deprecated")
@InternalApi
private[fcm] class FcmSender {
  import FcmJsonSupport._

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmSender */
  @deprecated("Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmSender", "Alpakka 3.0.2")
  @Deprecated
  def send(http: HttpExt, fcmSend: FcmSend)(
      implicit mat: Materializer,
      settings: GoogleSettings): Future[FcmResponse] = {
    import mat.executionContext
    import settings.projectId
    val url = s"https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

    Marshal(fcmSend).to[RequestEntity].flatMap { entity =>
      GoogleHttp(http)
        .singleAuthenticatedRequest[FcmSuccessResponse](HttpRequest(HttpMethods.POST, url, entity = entity))
    }.recover {
      case FcmErrorException(error) => error
    }
  }

  implicit private val unmarshaller: FromResponseUnmarshaller[FcmSuccessResponse] = Unmarshaller.withMaterializer {
    implicit ec => implicit mat => (response: HttpResponse) =>
      if (response.status.isSuccess) {
        Unmarshal(response.entity).to[FcmSuccessResponse]
      } else {
        Unmarshal(response.entity).to[FcmErrorResponse].map(error => throw FcmErrorException(error))
      }
  }.withDefaultRetry

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmErrorException */
  @deprecated("Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmErrorException", "Alpakka 3.0.2")
  @Deprecated
  private case class FcmErrorException(error: FcmErrorResponse) extends Exception
}
