/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.stream.connectors.geode.impl.pdx

import org.apache.pekko.annotation.InternalApi
import shapeless._
import shapeless.labelled._

import scala.util.{ Failure, Success }

@InternalApi
private[pekko] trait ObjectDecoder {

  implicit val hnilDecoder: PdxDecoder[HNil] = PdxDecoder.instance((_, _) => Success(HNil))

  implicit def hlistDecoder[K <: Symbol, H, T <: HList](
      implicit witness: Witness.Aux[K],
      hDecoder: Lazy[PdxDecoder[H]],
      tDecoder: Lazy[PdxDecoder[T]]): PdxDecoder[FieldType[K, H] :: T] = PdxDecoder.instance {
    case (reader, fieldName) =>
      val headField = hDecoder.value.decode(reader, witness.value)
      val tailFields = tDecoder.value.decode(reader, fieldName)
      (headField, tailFields) match {
        case (Success(h), Success(t)) => Success(field[K](h) :: t)
        case _                        => Failure(null)
      }
    case _ => Failure(null)
  }

  implicit def objectDecoder[A, Repr <: HList](
      implicit gen: LabelledGeneric.Aux[A, Repr],
      hlistDecoder: PdxDecoder[Repr]): PdxDecoder[A] = PdxDecoder.instance { (reader, fieldName) =>
    hlistDecoder.decode(reader, fieldName).map(gen.from)
  }

}
