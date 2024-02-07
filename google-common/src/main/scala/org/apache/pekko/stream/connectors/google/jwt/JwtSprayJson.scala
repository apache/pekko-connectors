/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.stream.connectors.google.jwt

import java.time.Clock
import org.apache.pekko
import pekko.annotation.InternalApi
import pdi.jwt._
import pdi.jwt.exceptions.JwtNonStringException
import spray.json._

/**
 * Implementation of `JwtCore` using `JsObject` from spray-json.
 *
 * This is a copy of Apache licensed (version 2.0) code from
 * https://github.com/jwt-scala/jwt-scala/blob/224f16124ea49a1cc5144a647e3767de4267ee7c/json/spray-json/src/main/scala/JwtSprayJson.scala
 */
@InternalApi
private[google] trait JwtSprayJsonParser[H, C] extends JwtJsonCommon[JsObject, H, C] {
  protected def parse(value: String): JsObject = value.parseJson.asJsObject

  protected def stringify(value: JsObject): String = value.compactPrint

  protected def getAlgorithm(header: JsObject): Option[JwtAlgorithm] =
    header.fields.get("alg").flatMap {
      case JsString("none") => None
      case JsString(algo)   => Option(JwtAlgorithm.fromString(algo))
      case JsNull           => None
      case _                => throw new JwtNonStringException("alg")
    }

}

@InternalApi
private[google] object JwtSprayJson {
  def apply(clock: Clock): JwtSprayJson = new JwtSprayJson(clock)
}

@InternalApi
private[google] class JwtSprayJson private (defaultClock: Clock)
    extends JwtSprayJsonParser[JwtHeader, JwtClaim] {
  import DefaultJsonProtocol._
  implicit val clock: Clock = defaultClock

  override def parseHeader(header: String): JwtHeader = {
    val jsObj = parse(header)
    JwtHeader(
      algorithm = getAlgorithm(jsObj),
      typ = safeGetField[String](jsObj, "typ"),
      contentType = safeGetField[String](jsObj, "cty"),
      keyId = safeGetField[String](jsObj, "kid"))
  }

  override def parseClaim(claim: String): JwtClaim = {
    val jsObj = parse(claim)
    val content = JsObject(
      jsObj.fields - "iss" - "sub" - "aud" - "exp" - "nbf" - "iat" - "jti")
    JwtClaim(
      content = stringify(content),
      issuer = safeGetField[String](jsObj, "iss"),
      subject = safeGetField[String](jsObj, "sub"),
      audience = safeGetField[Set[String]](jsObj, "aud")
        .orElse(safeGetField[String](jsObj, "aud").map(s => Set(s))),
      expiration = safeGetField[Long](jsObj, "exp"),
      notBefore = safeGetField[Long](jsObj, "nbf"),
      issuedAt = safeGetField[Long](jsObj, "iat"),
      jwtId = safeGetField[String](jsObj, "jti"))
  }

  private[this] def safeRead[A: JsonReader](js: JsValue) =
    safeReader[A].read(js).fold(_ => None, a => Option(a))

  private[this] def safeGetField[A: JsonReader](js: JsObject, name: String) =
    js.fields.get(name).flatMap(safeRead[A])
}
