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
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv._
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.couchbase3.{ MutationBinaryDocument, MutationDocument }
import org.apache.pekko.stream.javadsl.Flow
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.{ CouchbaseFlow => ScalaCouchbaseFlow }

import java.time.{ Duration, Instant }
import java.util.function.{ Function => JFunction }

object CouchbaseFlow {

  /**
   * get a document by id from Couchbase collection
   * @param options reference to Couchbase options doc
   */
  def get(options: GetOptions,
      asyncCollection: AsyncCollection): Flow[String, GetResult, NotUsed] =
    ScalaCouchbaseFlow.get(options)(asyncCollection).asJava

  /**
   * get a document by id from Couchbase collection
   * @param options reference to Couchbase options doc
   */
  def get(asyncCollection: AsyncCollection): Flow[String, GetResult, NotUsed] =
    ScalaCouchbaseFlow.get(GetOptions.getOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.get]] deserialize to Couchbase JsonObject
   */
  def getJson(options: GetOptions,
      asyncCollection: AsyncCollection): Flow[String, JsonObject, NotUsed] =
    ScalaCouchbaseFlow.getJson(options)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.get]] deserialize to Couchbase JsonObject
   */
  def getJson(asyncCollection: AsyncCollection): Flow[String, JsonObject, NotUsed] =
    ScalaCouchbaseFlow.getJson(GetOptions.getOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.get]],deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getObject[T](target: Class[T], options: GetOptions,
      asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getObject[T](target, options)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.get]],deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getObject[T](target: Class[T], asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getObject[T](target, GetOptions.getOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseSource.getObject]],deserialize to class with Generics
   */
  def getType[T](target: TypeRef[T], options: GetOptions,
      asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getType[T](target, options)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseSource.getObject]],deserialize to class with Generics
   */
  def getType[T](target: TypeRef[T], asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getType[T](target, GetOptions.getOptions)(asyncCollection).asJava

  /**
   * similar to [[CouchbaseFlow.get]], but reads from all replicas on the active node
   * @see [[CouchbaseFlow#get]]
   */
  def getAllReplicas(options: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Flow[String, GetReplicaResult, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicas(options)(asyncCollection).asJava

  /**
   * similar to [[CouchbaseFlow.get]], but reads from all replicas on the active node
   * @see [[CouchbaseFlow#get]]
   */
  def getAllReplicas(asyncCollection: AsyncCollection): Flow[String, GetReplicaResult, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicas(GetAllReplicasOptions.getAllReplicasOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicas]], deserialize to Couchbase JsonObject
   */
  def getAllReplicasJson(options: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Flow[String, JsonObject, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasJson(options)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicas]], deserialize to Couchbase JsonObject
   */
  def getAllReplicasJson(asyncCollection: AsyncCollection): Flow[String, JsonObject, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasJson(GetAllReplicasOptions.getAllReplicasOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicas]], deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getAllReplicasObject[T](target: Class[T],
      getOptions: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasObject[T](target, getOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicas]], deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getAllReplicasObject[T](target: Class[T],
      asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasObject[T](target, GetAllReplicasOptions.getAllReplicasOptions)(
      asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicasObject]], deserialize to class with Generics
   */
  def getAllReplicasType[T](target: TypeRef[T],
      getOptions: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasType(target, getOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicasObject]], deserialize to class with Generics
   */
  def getAllReplicasType[T](target: TypeRef[T],
      asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasType(target, GetAllReplicasOptions.getAllReplicasOptions)(asyncCollection).asJava

  /**
   * Inserts a full document which does not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#insert]]
   */
  def insert[T](applyId: JFunction[T, String],
      insertOptions: InsertOptions,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.insert[T](applyId.apply, insertOptions)(asyncCollection).asJava

  /**
   * Inserts a full document which does not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#insert]]
   */
  def insert[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.insert[T](applyId.apply)(asyncCollection).asJava

  /**
   * Inserts a full document which does not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#insert]]
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def insert[T](applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.insert[T](applyId, insertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.insert]] <br>
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def insertDoc[T](insertOptions: InsertOptions,
      asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.insertDoc[T](insertOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.insert]] <br>
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def insertDoc[T](asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.insertDoc[T](InsertOptions.insertOptions())(asyncCollection).asJava

  /**
   * Replaces a full document which already exists with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#replace]]
   */
  def replace[T](applyId: JFunction[T, String],
      replaceOptions: ReplaceOptions,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.replace[T](applyId.apply, replaceOptions)(asyncCollection).asJava

  /**
   * Replaces a full document which already exists with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#replace]]
   */
  def replace[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.replace[T](applyId.apply)(asyncCollection).asJava

  /**
   * Replaces a full document which already exists with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#replace]]
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def replace[T](applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.replace[T](applyId, replaceOptions).asJava

  /**
   * reference to [[CouchbaseFlow.replace]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def replaceDoc[T](replaceOptions: ReplaceOptions,
      asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.replaceDoc[T](replaceOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.replace]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def replaceDoc[T](asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.replaceDoc[T](ReplaceOptions.replaceOptions())(asyncCollection).asJava

  /**
   * Upsert a full document which might or might not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#upsert]]
   */
  def upsert[T](applyId: JFunction[T, String],
      upsertOptions: UpsertOptions,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.upsert[T](applyId.apply, upsertOptions)(asyncCollection).asJava

  /**
   * Upsert a full document which might or might not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#upsert]]
   */
  def upsert[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.upsert[T](applyId.apply)(asyncCollection).asJava

  /**
   * Upsert a full document which might or might not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#upsert]]
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def upsert[T](applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.upsert[T](applyId, upsertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.upsert]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def upsertDoc[T](
      upsertOptions: UpsertOptions,
      asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.upsertDoc[T](upsertOptions)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.upsert]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def upsertDoc[T](
      asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.upsertDoc[T](UpsertOptions.upsertOptions())(asyncCollection).asJava

  /**
   * Removes a Document from a collection with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `remove[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#remove]]
   */
  def remove[T](
      applyId: JFunction[T, String],
      removeOptions: RemoveOptions,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.remove[T](applyId.apply, removeOptions)(asyncCollection).asJava

  /**
   * Removes a Document from a collection with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `remove[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#remove]]
   */
  def remove[T](
      applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.remove[T](applyId.apply)(asyncCollection).asJava

  /**
   * Removes a Document from a collection with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `remove[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#remove]]
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def remove[T](
      applyId: T => String,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.remove[T](applyId, removeOptions).asJava

  /**
   * Performs mutations to document fragments with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#mutateIn]]
   */
  def mutateIn(specs: java.util.List[MutateInSpec], options: MutateInOptions,
      asyncCollection: AsyncCollection): Flow[String, MutateInResult, NotUsed] =
    ScalaCouchbaseFlow.mutateIn(specs, options)(asyncCollection).asJava

  /**
   * Performs mutations to document fragments with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#mutateIn]]
   */
  def mutateIn(specs: java.util.List[MutateInSpec],
      asyncCollection: AsyncCollection): Flow[String, MutateInResult, NotUsed] =
    ScalaCouchbaseFlow.mutateIn(specs, MutateInOptions.mutateInOptions())(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.mutateIn]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   * @return
   */
  def mutateInDoc[T](
      specs: java.util.List[MutateInSpec],
      options: MutateInOptions,
      asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.mutateInDoc[T](specs, options)(asyncCollection).asJava

  /**
   * reference to [[CouchbaseFlow.mutateIn]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   * @return
   */
  def mutateInDoc[T](
      specs: java.util.List[MutateInSpec],
      asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.mutateInDoc[T](specs, MutateInOptions.mutateInOptions())(asyncCollection).asJava

  /**
   * Checks if the given document ID exists on the active partition with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `exists[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#exists]]
   */
  def exists[T](
      applyId: JFunction[T, String],
      existsOptions: ExistsOptions,
      asyncCollection: AsyncCollection): Flow[T, Boolean, NotUsed] =
    ScalaCouchbaseFlow.exists[T](applyId.apply, existsOptions)(asyncCollection).asJava

  /**
   * Checks if the given document ID exists on the active partition with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `exists[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#exists]]
   */
  def exists[T](
      applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Flow[T, Boolean, NotUsed] =
    ScalaCouchbaseFlow.exists[T](applyId.apply)(asyncCollection).asJava

  /**
   * Checks if the given document ID exists on the active partition with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `exists[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#exists]]
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def exists[T](
      applyId: T => String)(
      implicit asyncCollection: AsyncCollection): Flow[T, Boolean, NotUsed] =
    ScalaCouchbaseFlow.exists[T](applyId).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#touch]]
   */
  def touch(expiry: Duration, options: TouchOptions,
      asyncCollection: AsyncCollection): Flow[String, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.touch(expiry, options)(asyncCollection).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#touch]]
   */
  def touch(expiry: Duration,
      asyncCollection: AsyncCollection): Flow[String, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.touch(expiry, TouchOptions.touchOptions())(asyncCollection).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @param applyId parse id function, which is the document id
   */
  def touchDuration[T](
      applyId: JFunction[T, String],
      expiry: Duration,
      touchOptions: TouchOptions,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchDuration[T](applyId.apply, expiry, touchOptions)(asyncCollection).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @param applyId parse id function, which is the document id
   */
  def touchDuration[T](
      applyId: JFunction[T, String],
      expiry: Duration,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchDuration[T](applyId.apply, expiry)(asyncCollection).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @param applyId parse id function, which is the document id
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def touchDuration[T](
      applyId: T => String,
      expiry: Duration,
      touchOptions: TouchOptions = TouchOptions.touchOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchDuration[T](applyId, expiry, touchOptions).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#touch]]
   */
  def touchInstant[T](
      applyId: JFunction[T, String],
      expiry: Instant,
      touchOptions: TouchOptions,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchInstant[T](applyId.apply, expiry, touchOptions)(asyncCollection).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#touch]]
   */
  def touchInstant[T](
      applyId: JFunction[T, String],
      expiry: Instant,
      asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchInstant[T](applyId.apply, expiry)(asyncCollection).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#touch]]
   * @deprecated Use the overloaded method that takes a java.util.function.Function instead (since 2.0.0)
   */
  @deprecated("Use the overloaded method that takes a java.util.function.Function instead", since = "2.0.0")
  def touchInstant[T](
      applyId: T => String,
      expiry: Instant,
      touchOptions: TouchOptions = TouchOptions.touchOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchInstant[T](applyId, expiry, touchOptions).asJava

  /**
   * Appends binary content to the document with custom options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#append]]
   */
  def append(options: AppendOptions,
      asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.append(options)(asyncCollection).asJava

  /**
   * Appends binary content to the document with custom options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#append]]
   */
  def append(asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.append(AppendOptions.appendOptions())(asyncCollection).asJava

  /**
   * Prepends binary content to the document with custom options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#prepend]]
   */
  def prepend(options: PrependOptions,
      asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.prepend(options)(asyncCollection).asJava

  /**
   * Prepends binary content to the document with custom options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#prepend]]
   */
  def prepend(asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.prepend(PrependOptions.prependOptions())(asyncCollection).asJava

  /**
   * Increments the counter document by one or the number defined in the options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#increment]]
   */
  def increment(options: IncrementOptions,
      asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] =
    ScalaCouchbaseFlow.increment(options)(asyncCollection).asJava

  /**
   * Increments the counter document by one or the number defined in the options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#increment]]
   */
  def increment(asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] =
    ScalaCouchbaseFlow.increment(IncrementOptions.incrementOptions())(asyncCollection).asJava

  /**
   * Decrements the counter document by one or the number defined in the options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#decrement]]
   */
  def decrement(options: DecrementOptions,
      asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] =
    ScalaCouchbaseFlow.decrement(options)(asyncCollection).asJava

  /**
   * Decrements the counter document by one or the number defined in the options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#decrement]]
   */
  def decrement(asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] =
    ScalaCouchbaseFlow.decrement(DecrementOptions.decrementOptions())(asyncCollection).asJava

}
