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
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.stream.connectors.google.firebase.fcm.{ FcmErrorResponse, FcmResponse, FcmSuccessResponse }
import pekko.annotation.InternalApi
import pekko.stream.connectors.google.firebase.fcm.FcmNotification
import pekko.stream.connectors.google.firebase.fcm.FcmNotificationModels._
import spray.json._

/**
 * INTERNAL API
 */
@InternalApi
@deprecated("Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmSend", "Alpakka 3.0.2")
@Deprecated
private[fcm] case class FcmSend(validate_only: Boolean, message: FcmNotification)

/**
 * INTERNAL API
 */
@InternalApi
@deprecated("Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmJsonSupport", "Alpakka 3.0.2")
@Deprecated
private[fcm] object FcmJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {

  // custom formatters
  implicit object FcmSuccessResponseJsonFormat extends RootJsonFormat[FcmSuccessResponse] {
    def write(c: FcmSuccessResponse): JsValue = JsString(c.name)

    def read(value: JsValue) = value match {
      case JsObject(fields) if fields.contains("name") => FcmSuccessResponse(fields("name").convertTo[String])
      case other                                       => throw DeserializationException(s"object containing `name` expected, but we get $other")
    }
  }
  implicit object FcmErrorResponseJsonFormat extends RootJsonFormat[FcmErrorResponse] {
    def write(c: FcmErrorResponse): JsValue = c.rawError.parseJson
    def read(value: JsValue) = FcmErrorResponse(value.toString)
  }

  implicit object FcmResponseFormat extends RootJsonReader[FcmResponse] {
    def read(value: JsValue): FcmResponse = value match {
      case JsObject(fields) if fields.keys.exists(_ == "name")       => value.convertTo[FcmSuccessResponse]
      case JsObject(fields) if fields.keys.exists(_ == "error_code") => value.convertTo[FcmErrorResponse]
      case other                                                     => throw DeserializationException(s"FcmResponse expected, but we get $other")
    }
  }

  implicit object AndroidMessagePriorityFormat extends RootJsonFormat[AndroidMessagePriority] {
    def write(c: AndroidMessagePriority): JsString =
      c match {
        case Normal => JsString("NORMAL")
        case High   => JsString("HIGH")
      }

    def read(value: JsValue): AndroidMessagePriority = value match {
      case JsString("NORMAL") => Normal
      case JsString("HIGH")   => High
      case other              => throw DeserializationException(s"AndroidMessagePriority expected, but we get $other")
    }
  }

  implicit object ApnsConfigResponseJsonFormat extends RootJsonFormat[ApnsConfig] {
    def write(c: ApnsConfig): JsObject =
      JsObject(
        "headers" -> c.headers.toJson,
        "payload" -> c.rawPayload.parseJson)

    def read(value: JsValue): ApnsConfig = {
      val map = value.asJsObject
      ApnsConfig(map.fields("headers").convertTo[Map[String, String]], map.fields("payload").toString)
    }
  }

  // app -> google
  implicit val webPushNotificationJsonFormat: RootJsonFormat[WebPushNotification] = jsonFormat3(WebPushNotification)
  implicit val webPushConfigJsonFormat: RootJsonFormat[WebPushConfig] = jsonFormat3(WebPushConfig.apply)
  implicit val androidNotificationJsonFormat: RootJsonFormat[AndroidNotification] = jsonFormat11(AndroidNotification)
  implicit val androidConfigJsonFormat: RootJsonFormat[AndroidConfig] = jsonFormat6(AndroidConfig.apply)
  implicit val basicNotificationJsonFormat: RootJsonFormat[BasicNotification] = jsonFormat2(BasicNotification)
  implicit val sendableFcmNotificationJsonFormat: RootJsonFormat[FcmNotification] = jsonFormat8(FcmNotification.apply)
  implicit val fcmSendJsonFormat: RootJsonFormat[FcmSend] = jsonFormat2(FcmSend)
}
