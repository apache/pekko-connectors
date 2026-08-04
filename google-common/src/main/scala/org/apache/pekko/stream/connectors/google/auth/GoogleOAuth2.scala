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

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.annotation.InternalApi
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.http.scaladsl.model.HttpMethods.POST
import pekko.http.scaladsl.model.{ FormData, HttpRequest }
import pekko.stream.Materializer
import pekko.stream.connectors.google.http.GoogleHttp
import pekko.stream.connectors.google.jwt.JwtSprayJson
import pekko.stream.connectors.google.{ implicits, RequestSettings }
import pdi.jwt.{ JwtClaim, JwtTime }
import pdi.jwt.JwtAlgorithm.RS256
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

import java.time.Clock
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

@InternalApi
private[auth] object GoogleOAuth2 {

  private val oAuthTokenUrl = "https://oauth2.googleapis.com/token"
  private[auth] val DefaultExpiresIn = 3600

  def getAccessToken(clientEmail: String, privateKey: String, scopes: Set[String])(
      implicit mat: Materializer,
      settings: RequestSettings,
      clock: Clock): Future[AccessToken] = {
    import GoogleOAuth2Exception._
    import SprayJsonSupport._
    import implicits._
    implicit val system: ActorSystem = mat.system

    try {
      val entity = FormData(
        "grant_type" -> "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion" -> generateJwt(clientEmail, privateKey, scopes)).toEntity

      GoogleHttp().singleRequest[AccessTokenResponse](HttpRequest(POST, oAuthTokenUrl, entity = entity)).map {
        case AccessTokenResponse(access_token, _, expires_in) =>
          val safeExpiresIn = if (expires_in > 0) expires_in
          else {
            system.log.warning("Google OAuth2 response contained invalid expires_in ({}), falling back to default {}s",
              expires_in, DefaultExpiresIn)
            DefaultExpiresIn
          }
          AccessToken(access_token, JwtTime.nowSeconds + safeExpiresIn)
      }(ExecutionContext.parasitic)
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  private def generateJwt(clientEmail: String, privateKey: String, scopes: Set[String])(
      implicit clock: Clock): String = {
    import spray.json._

    val scope = scopes.mkString(" ")
    val claim = JwtClaim(content = JwtClaimContent(scope).toJson.compactPrint,
      audience = Some(Set(oAuthTokenUrl)),
      issuer = Some(clientEmail))
      .expiresIn(3600)
      .issuedNow

    JwtSprayJson(clock).encode(claim, privateKey, RS256)
  }

  final case class JwtClaimContent(scope: String)
  implicit val jwtClaimContentFormat: JsonFormat[JwtClaimContent] = jsonFormat1(JwtClaimContent.apply)
}
