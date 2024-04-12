/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
private[google] object JwtSprayJson extends JwtSprayJsonParser[JwtHeader, JwtClaim] {
  import DefaultJsonProtocol._

  def apply(clock: Clock): JwtSprayJson = new JwtSprayJson(clock)

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

@InternalApi
private[google] class JwtSprayJson private (override val clock: Clock)
    extends JwtSprayJsonParser[JwtHeader, JwtClaim] {
  override def parseHeader(header: String): JwtHeader = JwtSprayJson.parseHeader(header)
  override def parseClaim(header: String): JwtClaim = JwtSprayJson.parseClaim(header)
}
