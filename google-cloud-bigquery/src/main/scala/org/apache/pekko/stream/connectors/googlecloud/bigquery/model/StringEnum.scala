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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.model

import org.apache.pekko
import pekko.annotation.InternalApi
import spray.json.{ deserializationError, JsString, JsValue, JsonFormat }

import scala.reflect.ClassTag

@InternalApi
private[model] abstract class StringEnum {
  def value: String
  final def getValue: String = value
}

@InternalApi
private[model] object StringEnum {
  def jsonFormat[T <: StringEnum](f: String => T)(implicit ct: ClassTag[T]): JsonFormat[T] = new JsonFormat[T] {
    override def write(obj: T): JsValue = JsString(obj.value)
    override def read(json: JsValue): T = json match {
      case JsString(x) => f(x)
      case x           => deserializationError(s"Expected ${ct.runtimeClass.getSimpleName} as JsString, but got " + x)
    }
  }
}
