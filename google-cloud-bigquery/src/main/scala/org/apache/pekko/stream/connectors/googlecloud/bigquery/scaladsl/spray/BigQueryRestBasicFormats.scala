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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray

import spray.json.{ deserializationError, DefaultJsonProtocol, JsNumber, JsValue, JsonFormat }

import scala.concurrent.duration.{ DurationLong, FiniteDuration }

/**
 * Provides the JsonFormats for the BigQuery REST API's representation of the most important Scala types.
 */
trait BigQueryRestBasicFormats {

  implicit val IntJsonFormat: JsonFormat[Int] = DefaultJsonProtocol.IntJsonFormat
  implicit val FloatJsonFormat: JsonFormat[Float] = DefaultJsonProtocol.FloatJsonFormat
  implicit val DoubleJsonFormat: JsonFormat[Double] = DefaultJsonProtocol.DoubleJsonFormat
  implicit val ByteJsonFormat: JsonFormat[Byte] = DefaultJsonProtocol.ByteJsonFormat
  implicit val ShortJsonFormat: JsonFormat[Short] = DefaultJsonProtocol.ShortJsonFormat
  implicit val BigDecimalJsonFormat: JsonFormat[BigDecimal] = DefaultJsonProtocol.BigDecimalJsonFormat
  implicit val BigIntJsonFormat: JsonFormat[BigInt] = DefaultJsonProtocol.BigIntJsonFormat
  implicit val UnitJsonFormat: JsonFormat[Unit] = DefaultJsonProtocol.UnitJsonFormat
  implicit val BooleanJsonFormat: JsonFormat[Boolean] = DefaultJsonProtocol.BooleanJsonFormat
  implicit val CharJsonFormat: JsonFormat[Char] = DefaultJsonProtocol.CharJsonFormat
  implicit val StringJsonFormat: JsonFormat[String] = DefaultJsonProtocol.StringJsonFormat
  implicit val SymbolJsonFormat: JsonFormat[Symbol] = DefaultJsonProtocol.SymbolJsonFormat

  implicit object BigQueryLongJsonFormat extends JsonFormat[Long] {
    def write(x: Long): JsNumber = JsNumber(x)
    def read(value: JsValue): Long = value match {
      case JsNumber(x) if x.isValidLong       => x.longValue
      case BigQueryNumber(x) if x.isValidLong => x.longValue
      case x                                  => deserializationError("Expected Long as JsNumber or JsString, but got " + x)
    }
  }

  implicit object BigQueryFiniteDurationJsonFormat extends JsonFormat[FiniteDuration] {
    override def write(x: FiniteDuration): JsValue = JsNumber(x.toMillis)
    override def read(value: JsValue): FiniteDuration = value match {
      case JsNumber(x) if x.isValidLong       => x.longValue.millis
      case BigQueryNumber(x) if x.isValidLong => x.longValue.millis
      case x                                  => deserializationError("Expected FiniteDuration as JsNumber or JsString, but got " + x)
    }
  }
}
