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

package org.apache.pekko.stream.connectors.geode.impl.pdx

import org.apache.pekko.annotation.InternalApi

import scala.deriving.Mirror

@InternalApi
private[pekko] type FieldType[K, +V] = V with KeyTag[K, V]

@InternalApi
private[pekko] object FieldType {
  def label[K]: [V] => V => FieldType[K, V] = [V] => (v: V) => v.asInstanceOf[FieldType[K, V]]
}

@InternalApi
private[pekko] type KeyTag[K, +V]

@InternalApi
private[pekko] type ZipWith[T1 <: Tuple, T2 <: Tuple, F[_, _]] <: Tuple = (T1, T2) match {
  case (h1 *: t1, h2 *: t2) => F[h1, h2] *: ZipWith[t1, t2, F]
  case (EmptyTuple, ?)      => EmptyTuple
  case (?, EmptyTuple)      => EmptyTuple
  case _                    => Tuple
}

@InternalApi
private[pekko] trait LabelledGeneric[A] {
  type Repr
  def from(r: Repr): A
  def to(a: A): Repr
}

@InternalApi
private[pekko] object LabelledGeneric {
  type Aux[A, R] = LabelledGeneric[A] { type Repr = R }

  inline def apply[A](using l: LabelledGeneric[A]): LabelledGeneric.Aux[A, l.Repr] = l

  transparent inline given productInst[A <: Product](
      using m: Mirror.ProductOf[A])
      : LabelledGeneric.Aux[A, ZipWith[m.MirroredElemLabels, m.MirroredElemTypes, FieldType]] =
    new LabelledGeneric[A] {
      type Repr = Tuple & ZipWith[m.MirroredElemLabels, m.MirroredElemTypes, FieldType]
      def from(r: Repr): A = m.fromTuple(r.asInstanceOf[m.MirroredElemTypes])
      def to(a: A): Repr = Tuple.fromProductTyped(a).asInstanceOf[Repr]
    }
}
