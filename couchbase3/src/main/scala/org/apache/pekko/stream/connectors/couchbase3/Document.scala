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

package org.apache.pekko.stream.connectors.couchbase3

import java.util.Optional

import com.couchbase.client.java.kv.MutationResult

import scala.jdk.OptionConverters._

case class MutationDocument[T](id: String, doc: T, result: Option[MutationResult] = Option.empty) {

  def withResult(mutationResult: MutationResult) = {
    copy(result = Some(mutationResult))
  }

  /**
   * Java API: the result of the mutation, if it has been applied.
   *
   * @since 2.0.0
   */
  def getResult: Optional[MutationResult] = result.toJava
}

object MutationDocument {

  /**
   * Java API
   *
   * @since 2.0.0
   */
  def create[T](id: String, doc: T): MutationDocument[T] = MutationDocument(id, doc)
}

case class MutationBinaryDocument(id: String, doc: Array[Byte], result: Option[MutationResult] = Option.empty) {

  def withResult(mutationResult: MutationResult) = {
    copy(result = Some(mutationResult))
  }

  /**
   * Java API: the result of the mutation, if it has been applied.
   *
   * @since 2.0.0
   */
  def getResult: Optional[MutationResult] = result.toJava
}

// extends AbstractFunction3 like the companion the compiler generates when there is no explicit one
object MutationBinaryDocument
    extends scala.runtime.AbstractFunction3[String, Array[Byte], Option[MutationResult], MutationBinaryDocument] {

  /**
   * Java API
   *
   * @since 2.0.0
   */
  def create(id: String, doc: Array[Byte]): MutationBinaryDocument = MutationBinaryDocument(id, doc)
}
