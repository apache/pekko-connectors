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
import shapeless.ops.hlist.IsHCons

@InternalApi
private[pekko] trait ObjectEncoder {

  implicit val hnilEncoder: PdxEncoder[HNil] =
    PdxEncoder.instance[HNil] { case _ => true }
  implicit def hlistEncoder[K <: Symbol, H, T <: shapeless.HList](
      implicit witness: Witness.Aux[K],
      isHCons: IsHCons.Aux[H :: T, H, T],
      hEncoder: Lazy[PdxEncoder[H]],
      tEncoder: Lazy[PdxEncoder[T]]): PdxEncoder[FieldType[K, H] :: T] =
    PdxEncoder.instance[FieldType[K, H] :: T] {
      case (writer, o, fieldName) =>
        hEncoder.value.encode(writer, isHCons.head(o), witness.value)
        tEncoder.value.encode(writer, isHCons.tail(o), fieldName)
    }

  implicit def objectEncoder[A, Repr <: HList](
      implicit gen: LabelledGeneric.Aux[A, Repr],
      hlistEncoder: Lazy[PdxEncoder[Repr]]): PdxEncoder[A] = PdxEncoder.instance {
    case (writer, o, fieldName) =>
      hlistEncoder.value.encode(writer, gen.to(o), fieldName)
  }

}
