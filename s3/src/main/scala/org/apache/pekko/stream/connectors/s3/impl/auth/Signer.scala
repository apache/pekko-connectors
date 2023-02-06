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

package org.apache.pekko.stream.connectors.s3.impl.auth

import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

import org.apache.pekko.NotUsed
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ HttpHeader, HttpRequest }
import org.apache.pekko.stream.scaladsl.Source

@InternalApi private[impl] object Signer {
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")

  def signedRequest(request: HttpRequest,
      key: SigningKey,
      signAnonymousRequests: Boolean): Source[HttpRequest, NotUsed] =
    if (!signAnonymousRequests && key.anonymous) Source.single(request)
    else {
      val hashedBody = request.entity.dataBytes.via(digest()).map(hash => encodeHex(hash.toArray))

      hashedBody
        .map { hb =>
          val headersToAdd = Vector(RawHeader("x-amz-date", key.requestDate.format(dateFormatter)),
            RawHeader("x-amz-content-sha256", hb)) ++ sessionHeader(key)
          val reqWithHeaders = request.withHeaders(request.headers ++ headersToAdd)
          val cr = CanonicalRequest.from(reqWithHeaders)
          val authHeader = authorizationHeader("AWS4-HMAC-SHA256", key, key.requestDate, cr)
          reqWithHeaders.withHeaders(reqWithHeaders.headers :+ authHeader)
        }
        .mapMaterializedValue(_ => NotUsed)
    }

  private[this] def sessionHeader(key: SigningKey): Option[HttpHeader] =
    key.sessionToken.map(RawHeader("X-Amz-Security-Token", _))

  private[this] def authorizationHeader(algorithm: String,
      key: SigningKey,
      requestDate: ZonedDateTime,
      canonicalRequest: CanonicalRequest): HttpHeader =
    RawHeader("Authorization", authorizationString(algorithm, key, requestDate, canonicalRequest))

  private[this] def authorizationString(algorithm: String,
      key: SigningKey,
      requestDate: ZonedDateTime,
      canonicalRequest: CanonicalRequest): String = {
    val sign = key.hexEncodedSignature(stringToSign(algorithm, key, requestDate, canonicalRequest).getBytes())
    s"$algorithm Credential=${key.credentialString}, SignedHeaders=${canonicalRequest.signedHeaders}, Signature=$sign"
  }

  def stringToSign(algorithm: String,
      signingKey: SigningKey,
      requestDate: ZonedDateTime,
      canonicalRequest: CanonicalRequest): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedRequest = encodeHex(digest.digest(canonicalRequest.canonicalString.getBytes()))
    val date = requestDate.format(dateFormatter)
    val scope = signingKey.scope.scopeString
    s"$algorithm\n$date\n$scope\n$hashedRequest"
  }

}
