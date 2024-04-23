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

object CouchbaseFlow {

  /**
   * get a document by id from Couchbase collection
   * @param options reference to Couchbase options doc
   */
  def get(options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, GetResult, NotUsed] =
    ScalaCouchbaseFlow.get(options).asJava

  /**
   * reference to [[CouchbaseFlow.get]] deserialize to Couchbase JsonObject
   */
  def getJson(options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, JsonObject, NotUsed] =
    ScalaCouchbaseFlow.getJson(options).asJava

  /**
   * reference to [[CouchbaseFlow.get]],deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getObject[T](target: Class[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getObject[T](target, options).asJava

  /**
   * reference to [[CouchbaseSource.getObject]],deserialize to class with Generics
   */
  def getType[T](target: TypeRef[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getType[T](target, options).asJava

  /**
   * same to Get option, but reads from all replicas on the active node
   * @see [[CouchbaseFlow#get]]
   */
  def getAllReplicas(options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, GetReplicaResult, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicas(options).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicas]], deserialize to Couchbase JsonObject
   */
  def getAllReplicasJson(options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, JsonObject, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasJson(options).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicas]], deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getAllReplicasObject[T](target: Class[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasObject[T](target, getOptions).asJava

  /**
   * reference to [[CouchbaseFlow.getAllReplicasObject]], deserialize to class with Generics
   */
  def getAllReplicasType[T](target: TypeRef[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    ScalaCouchbaseFlow.getAllReplicasType(target, getOptions).asJava

  /**
   * Inserts a full document which does not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#insert]]
   */
  def insert[T](applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.insert[T](applyId, insertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.insert]] <br>
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def insertDoc[T](insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.insertDoc[T](insertOptions).asJava

  /**
   * Replaces a full document which already exists with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#replace]]
   */
  def replace[T](applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.replace[T](applyId, replaceOptions).asJava

  /**
   * reference to [[CouchbaseFlow.replace]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def replaceDoc[T](replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.replaceDoc[T](replaceOptions).asJava

  /**
   * Upsert a full document which might or might not exist yet with custom options.
   * @param applyId parse id function, which is the document id
   * @see [[com.couchbase.client.java.AsyncCollection#upsert]]
   */
  def upsert[T](applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.upsert[T](applyId, upsertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.upsert]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   */
  def upsertDoc[T](
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.upsertDoc[T](upsertOptions).asJava

  /**
   * Removes a Document from a collection with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `remove[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#remove]]
   */
  def remove[T](
      applyId: T => String,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.remove[T](applyId, removeOptions).asJava

  /**
   * Performs mutations to document fragments with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#mutateIn]]
   */
  def mutateIn(specs: java.util.List[MutateInSpec], options: MutateInOptions = MutateInOptions.mutateInOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, MutateInResult, NotUsed] =
    ScalaCouchbaseFlow.mutateIn(specs, options).asJava

  /**
   * reference to [[CouchbaseFlow.mutateIn]]
   * use MutationDocument to wrapper id, document and result(MutationResult)
   * @return
   */
  def mutateInDoc[T](
      specs: java.util.List[MutateInSpec],
      options: MutateInOptions = MutateInOptions.mutateInOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    ScalaCouchbaseFlow.mutateInDoc[T](specs, options).asJava

  /**
   * Checks if the given document ID exists on the active partition with custom options.
   * @param applyId parse id function, which is the document id, id streams can use `exists[String](e => e)`
   * @see [[com.couchbase.client.java.AsyncCollection#exists]]
   */
  def exists[T](
      applyId: T => String,
      existsOptions: ExistsOptions = ExistsOptions.existsOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, Boolean, NotUsed] =
    ScalaCouchbaseFlow.exists[T](applyId, existsOptions).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @see [[com.couchbase.client.java.AsyncCollection#touch]]
   */
  def touch(expiry: Duration, options: TouchOptions = TouchOptions.touchOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.touch(expiry, options).asJava

  /**
   * Updates the expiry of the document with the given id with custom options.
   * @param applyId parse id function, which is the document id
   */
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
      applyId: T => String,
      expiry: Instant,
      touchOptions: TouchOptions = TouchOptions.touchOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    ScalaCouchbaseFlow.touchInstant[T](applyId, expiry, touchOptions).asJava

  /**
   * Appends binary content to the document with custom options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#append]]
   */
  def append(options: AppendOptions = AppendOptions.appendOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.append(options).asJava

  /**
   * Prepends binary content to the document with custom options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#prepend]]
   */
  def prepend(options: PrependOptions = PrependOptions.prependOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    ScalaCouchbaseFlow.prepend(options).asJava

  /**
   * Increments the counter document by one or the number defined in the options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#increment]]
   */
  def increment(options: IncrementOptions = IncrementOptions.incrementOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] =
    ScalaCouchbaseFlow.increment(options).asJava

  /**
   * Decrements the counter document by one or the number defined in the options.
   * @see [[com.couchbase.client.java.AsyncBinaryCollection#decrement]]
   */
  def decrement(options: DecrementOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] =
    ScalaCouchbaseFlow.decrement(options).asJava

}
