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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.marshalling.Marshal
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.unmarshalling.{ FromResponseUnmarshaller, Unmarshal, Unmarshaller }
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.google.firebase.fcm.v1.models.{
  FcmErrorResponse,
  FcmResponse,
  FcmSuccessResponse
}
import org.apache.pekko.stream.connectors.google.GoogleSettings
import org.apache.pekko.stream.connectors.google.http.GoogleHttp
import org.apache.pekko.stream.connectors.google.implicits._

import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[fcm] class FcmSender {
  import FcmJsonSupport._

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
    implicit ec => implicit mat => response: HttpResponse =>
      if (response.status.isSuccess) {
        Unmarshal(response.entity).to[FcmSuccessResponse]
      } else {
        Unmarshal(response.entity).to[FcmErrorResponse].map(error => throw FcmErrorException(error))
      }
  }.withDefaultRetry

  private case class FcmErrorException(error: FcmErrorResponse) extends Exception
}
