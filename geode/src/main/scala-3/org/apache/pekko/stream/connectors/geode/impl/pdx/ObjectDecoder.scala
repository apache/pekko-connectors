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

import scala.util.{ Failure, Success }

@InternalApi
private[pekko] trait ObjectDecoder {

  given emptyTupleDecoder: PdxDecoder[EmptyTuple] = PdxDecoder.instance((_, _) => Success(EmptyTuple))

  given tupleDecoder[K <: String, H, T <: Tuple](
      using m: ValueOf[K],
      hDecoder: PdxDecoder[H],
      tDecoder: PdxDecoder[T]): PdxDecoder[FieldType[K, H] *: T] = PdxDecoder.instance {
    case (reader, fieldName) => {
      val headField = hDecoder.decode(reader, Symbol(m.value))
      val tailFields = tDecoder.decode(reader, fieldName)
      (headField, tailFields) match {
        case (Success(h), Success(t)) => Success(FieldType.label[K](h) *: t)
        case _                        => Failure(null)
      }
    }
    case e => Failure(null)
  }

  given objectDecoder[A, Repr <: Tuple](
      using gen: LabelledGeneric.Aux[A, Repr],
      tupleDecoder: PdxDecoder[Repr]): PdxDecoder[A] = PdxDecoder.instance { (reader, fieldName) =>
    tupleDecoder.decode(reader, fieldName).map(gen.from)
  }
}
