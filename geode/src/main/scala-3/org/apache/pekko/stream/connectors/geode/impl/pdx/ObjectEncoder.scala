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

@InternalApi
private[pekko] trait ObjectEncoder {

  given emptyTupleEncoder: PdxEncoder[EmptyTuple] =
    PdxEncoder.instance[EmptyTuple] { case _ => true }

  given tupleEncoder[K <: String, H, T <: Tuple](using
      m: ValueOf[K],
      hEncoder: PdxEncoder[H],
      tEncoder: PdxEncoder[T]): PdxEncoder[FieldType[K, H] *: T] =
    PdxEncoder.instance[FieldType[K, H] *: T] {
      case (writer, o, fieldName) =>
        hEncoder.encode(writer, o.head, Symbol(m.value))
        tEncoder.encode(writer, o.tail, fieldName)
    }

  given objectEncoder[A, Repr <: Tuple](
      using gen: LabelledGeneric.Aux[A, Repr],
      tupleEncoder: PdxEncoder[Repr]): PdxEncoder[A] = PdxEncoder.instance {
    case (writer, o, fieldName) =>
      tupleEncoder.encode(writer, gen.to(o), fieldName)
  }
}
