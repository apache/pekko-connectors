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

package org.apache.pekko.stream.connectors.geode.impl.pdx

import java.util.{ Date, UUID }

import org.apache.pekko.annotation.InternalApi
import org.apache.geode.pdx.PdxReader

import scala.util.{ Success, Try }

@InternalApi
trait PdxDecoder[A] {

  def decode(reader: PdxReader, fieldName: Symbol = null): Try[A]

}

object PdxDecoder extends ObjectDecoder {

  private[pekko] def instance[A](f: (PdxReader, Symbol) => Try[A]): PdxDecoder[A] =
    (reader: PdxReader, fieldName: Symbol) => f(reader, fieldName)

  implicit val booleanDecoder: PdxDecoder[Boolean] = instance {
    case (reader, fieldName) =>
      Success(reader.readBoolean(fieldName.name))
  }

  implicit val booleanListDecoder: PdxDecoder[List[Boolean]] = instance {
    case (reader, fieldName) =>
      Success(reader.readBooleanArray(fieldName.name).toList)
  }

  implicit val booleanArrayDecoder: PdxDecoder[Array[Boolean]] = instance {
    case (reader, fieldName) =>
      Success(reader.readBooleanArray(fieldName.name))
  }

  implicit val intDecoder: PdxDecoder[Int] = instance { (reader, fieldName) =>
    Success(reader.readInt(fieldName.name))
  }

  implicit val intListDecoder: PdxDecoder[List[Int]] = instance { (reader, fieldName) =>
    Success(reader.readIntArray(fieldName.name).toList)
  }

  implicit val intArrayDecoder: PdxDecoder[Array[Int]] = instance { (reader, fieldName) =>
    Success(reader.readIntArray(fieldName.name))
  }

  implicit val doubleDecoder: PdxDecoder[Double] = instance { (reader, fieldName) =>
    Success(reader.readDouble(fieldName.name))
  }

  implicit val doubleListDecoder: PdxDecoder[List[Double]] = instance { (reader, fieldName) =>
    Success(reader.readDoubleArray(fieldName.name).toList)
  }

  implicit val doubleArrayDecoder: PdxDecoder[Array[Double]] = instance { (reader, fieldName) =>
    Success(reader.readDoubleArray(fieldName.name))
  }

  implicit val floatDecoder: PdxDecoder[Float] = instance { (reader, fieldName) =>
    Success(reader.readFloat(fieldName.name))
  }

  implicit val floatListDecoder: PdxDecoder[List[Float]] = instance { (reader, fieldName) =>
    Success(reader.readFloatArray(fieldName.name).toList)
  }
  implicit val floatArrayDecoder: PdxDecoder[Array[Float]] = instance { (reader, fieldName) =>
    Success(reader.readFloatArray(fieldName.name))
  }

  implicit val longDecoder: PdxDecoder[Long] = instance { (reader, fieldName) =>
    Success(reader.readLong(fieldName.name))
  }

  implicit val longListDecoder: PdxDecoder[List[Long]] = instance { (reader, fieldName) =>
    Success(reader.readLongArray(fieldName.name).toList)
  }
  implicit val longArrayDecoder: PdxDecoder[Array[Long]] = instance { (reader, fieldName) =>
    Success(reader.readLongArray(fieldName.name))
  }

  implicit val charDecoder: PdxDecoder[Char] = instance {
    case (reader, fieldName) =>
      Success(reader.readChar(fieldName.name))
  }

  implicit val charListDecoder: PdxDecoder[List[Char]] = instance {
    case (reader, fieldName) =>
      Success(reader.readCharArray(fieldName.name).toList)
  }
  implicit val charArrayDecoder: PdxDecoder[Array[Char]] = instance {
    case (reader, fieldName) =>
      Success(reader.readCharArray(fieldName.name))
  }

  implicit val stringDecoder: PdxDecoder[String] = instance {
    case (reader, fieldName) =>
      Success(reader.readString(fieldName.name))
  }

  implicit val stringListDecoder: PdxDecoder[List[String]] = instance {
    case (reader, fieldName) =>
      Success(reader.readStringArray(fieldName.name).toList)
  }

  implicit val stringArrayDecoder: PdxDecoder[Array[String]] = instance {
    case (reader, fieldName) =>
      Success(reader.readStringArray(fieldName.name))
  }

  implicit val dategDecoder: PdxDecoder[Date] = instance {
    case (reader, fieldName) =>
      Success(reader.readDate(fieldName.name))
  }

  implicit val uuidDecoder: PdxDecoder[UUID] = instance {
    case (reader, fieldName) =>
      Try(UUID.fromString(reader.readString(fieldName.name)))
  }

  implicit def listDecoder[T <: AnyRef]: PdxDecoder[List[T]] = instance {
    case (reader, fieldName) =>
      Try(reader.readObjectArray(fieldName.name).toList.asInstanceOf[List[T]])
  }

  implicit def setDecoder[T <: AnyRef]: PdxDecoder[Set[T]] = instance {
    case (reader, fieldName) =>
      Try(reader.readObjectArray(fieldName.name).toSet.asInstanceOf[Set[T]])
  }

  def apply[A](implicit ev: PdxDecoder[A]): PdxDecoder[A] = ev

}
