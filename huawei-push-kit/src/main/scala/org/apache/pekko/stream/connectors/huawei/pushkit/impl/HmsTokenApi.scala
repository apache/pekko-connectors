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

package org.apache.pekko.stream.connectors.huawei.pushkit.impl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.model.{ FormData, HttpMethods, HttpRequest }
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.huawei.pushkit.ForwardProxyHttpsContext.ForwardProxyHttpsContext
import org.apache.pekko.stream.connectors.huawei.pushkit.ForwardProxyPoolSettings.ForwardProxyPoolSettings
import HmsTokenApi.{ AccessTokenExpiry, OAuthResponse }
import org.apache.pekko.stream.connectors.huawei.pushkit.ForwardProxy
import pdi.jwt.JwtTime

import java.time.Clock
import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[pushkit] class HmsTokenApi(http: => HttpExt, system: ActorSystem, forwardProxy: Option[ForwardProxy]) {
  import PushKitJsonSupport._

  private val authUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"

  def now: Long = JwtTime.nowSeconds(Clock.systemUTC())

  def getAccessToken(clientId: String, privateKey: String)(
      implicit materializer: Materializer): Future[AccessTokenExpiry] = {
    import materializer.executionContext
    val expiresAt = now + 3600

    val requestEntity = FormData(
      "grant_type" -> "client_credentials",
      "client_secret" -> privateKey,
      "client_id" -> clientId).toEntity

    for {
      response <- forwardProxy match {
        case Some(fp) =>
          http.singleRequest(HttpRequest(HttpMethods.POST, authUrl, entity = requestEntity),
            connectionContext = fp.httpsContext(system),
            settings = fp.poolSettings(system))
        case None => http.singleRequest(HttpRequest(HttpMethods.POST, authUrl, entity = requestEntity))
      }
      result <- Unmarshal(response.entity).to[OAuthResponse]
    } yield {
      AccessTokenExpiry(
        accessToken = result.access_token,
        expiresAt = expiresAt)
    }
  }
}

/**
 * INTERNAL API
 */
@InternalApi
private[pushkit] object HmsTokenApi {
  case class AccessTokenExpiry(accessToken: String, expiresAt: Long)
  case class OAuthResponse(access_token: String, token_type: String, expires_in: Int)
}
