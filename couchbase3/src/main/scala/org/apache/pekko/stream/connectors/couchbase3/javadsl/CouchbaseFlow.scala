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

package org.apache.pekko.stream.connectors.couchbase3.javadsl

import com.couchbase.client.java.AsyncCollection
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.kv._
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.couchbase3.{ MutationBinaryDocument, MutationDocument }
import org.apache.pekko.stream.javadsl.Flow
import org.apache.pekko.stream.connectors.couchbase3.scaladsl

import java.time.{ Duration, Instant }

object CouchbaseFlow {

  def get(asyncCollection: AsyncCollection, getOptions: GetOptions = GetOptions.getOptions)
      : Flow[String, GetResult, NotUsed] =
    scaladsl.CouchbaseFlow.get(asyncCollection, getOptions).asJava

  def get[T](asyncCollection: AsyncCollection, target: Class[T],
      getOptions: GetOptions = GetOptions.getOptions): Flow[String, T, NotUsed] =
    scaladsl.CouchbaseFlow.get(asyncCollection, target, getOptions).asJava

  def get[T](asyncCollection: AsyncCollection, target: TypeRef[T],
      getOptions: GetOptions = GetOptions.getOptions): Flow[String, T, NotUsed] =
    scaladsl.CouchbaseFlow.get(asyncCollection, target, getOptions).asJava

  def getAllReplicas(asyncCollection: AsyncCollection,
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)
      : Flow[String, GetReplicaResult, NotUsed] =
    scaladsl.CouchbaseFlow.getAllReplicas(asyncCollection, getOptions).asJava

  def getAllReplicas[T](asyncCollection: AsyncCollection, target: Class[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Flow[String, T, NotUsed] =
    scaladsl.CouchbaseFlow.getAllReplicas(asyncCollection, target, getOptions).asJava

  def getAllReplicas[T](asyncCollection: AsyncCollection, target: TypeRef[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Flow[String, T, NotUsed] =
    scaladsl.CouchbaseFlow.getAllReplicas(asyncCollection, target, getOptions).asJava

  def insert[T](asyncCollection: AsyncCollection,
      insertOptions: InsertOptions = InsertOptions.insertOptions())
      : Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    scaladsl.CouchbaseFlow.insert(asyncCollection, insertOptions).asJava

  def insert[T](asyncCollection: AsyncCollection, applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions()): Flow[T, T, NotUsed] =
    scaladsl.CouchbaseFlow.insert(asyncCollection, applyId, insertOptions).asJava

  def replace[T](asyncCollection: AsyncCollection,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())
      : Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    scaladsl.CouchbaseFlow.replace(asyncCollection, replaceOptions).asJava

  def replace[T](asyncCollection: AsyncCollection, applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions()): Flow[T, T, NotUsed] =
    scaladsl.CouchbaseFlow.replace(asyncCollection, applyId, replaceOptions).asJava

  def upsert[T](asyncCollection: AsyncCollection,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())
      : Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    scaladsl.CouchbaseFlow.upsert(asyncCollection, upsertOptions).asJava

  def upsert[T](asyncCollection: AsyncCollection, applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Flow[T, T, NotUsed] =
    scaladsl.CouchbaseFlow.upsert(asyncCollection, applyId, upsertOptions).asJava

  def remove(asyncCollection: AsyncCollection,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions()): Flow[String, String, NotUsed] =
    scaladsl.CouchbaseFlow.remove(asyncCollection, removeOptions).asJava

  def exists(asyncCollection: AsyncCollection,
      existsOptions: ExistsOptions = ExistsOptions.existsOptions()): Flow[String, Boolean, NotUsed] =
    scaladsl.CouchbaseFlow.exists(asyncCollection, existsOptions).asJava

  def mutateIn(asyncCollection: AsyncCollection, specs: java.util.List[MutateInSpec],
      options: MutateInOptions=MutateInOptions.mutateInOptions()): Flow[String, MutateInResult, NotUsed] =
    scaladsl.CouchbaseFlow.mutateIn(asyncCollection, specs, options).asJava

  def touch(asyncCollection: AsyncCollection, expiry: Duration,
      touchOptions: TouchOptions = TouchOptions.touchOptions()): Flow[String, String, NotUsed] =
    scaladsl.CouchbaseFlow.touch(asyncCollection, expiry, touchOptions).asJava

  def touch(asyncCollection: AsyncCollection, expiry: Instant,
      touchOptions: TouchOptions = TouchOptions.touchOptions()): Flow[String, String, NotUsed] =
    scaladsl.CouchbaseFlow.touch(asyncCollection, expiry, touchOptions).asJava

  def append(asyncCollection: AsyncCollection,
      options: AppendOptions = AppendOptions.appendOptions()): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    scaladsl.CouchbaseFlow.append(asyncCollection, options).asJava

  def prepend(asyncCollection: AsyncCollection,
      options: PrependOptions = PrependOptions.prependOptions())
      : Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    scaladsl.CouchbaseFlow.prepend(asyncCollection, options).asJava

  def increment(asyncCollection: AsyncCollection,
      options: IncrementOptions = IncrementOptions.incrementOptions()): Flow[String, CounterResult, NotUsed] =
    scaladsl.CouchbaseFlow.increment(asyncCollection, options).asJava

  def decrement(asyncCollection: AsyncCollection,
      options: DecrementOptions = DecrementOptions.decrementOptions()): Flow[String, CounterResult, NotUsed] =
    scaladsl.CouchbaseFlow.decrement(asyncCollection, options).asJava

}
