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

package org.apache.pekko.stream.connectors.couchbase3.scaladsl

import com.couchbase.client.java.AsyncCollection
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.kv.{ AppendOptions, _ }
import java.time.{ Duration, Instant }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.couchbase3.{ MutationBinaryDocument, MutationDocument }
import org.apache.pekko.stream.scaladsl.{ Flow, Source }

object CouchbaseFlow {

  def get(asyncCollection: AsyncCollection, getOptions: GetOptions = GetOptions.getOptions)
      : Flow[String, GetResult, NotUsed] =
    Flow[String].flatMapConcat(CouchbaseSource.get(asyncCollection, _, getOptions))

  def get[T](asyncCollection: AsyncCollection, target: Class[T],
      getOptions: GetOptions = GetOptions.getOptions): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.get(asyncCollection, _, target, getOptions))

  def get[T](asyncCollection: AsyncCollection, target: TypeRef[T],
      getOptions: GetOptions = GetOptions.getOptions): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.get(asyncCollection, _, target, getOptions))

  def getAllReplicas(asyncCollection: AsyncCollection,
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)
      : Flow[String, GetReplicaResult, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getAllReplicas(asyncCollection, _, getOptions))

  def getAllReplicas[T](asyncCollection: AsyncCollection, target: Class[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getAllReplicas(asyncCollection, _, target, getOptions))

  def getAllReplicas[T](asyncCollection: AsyncCollection, target: TypeRef[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getAllReplicas(asyncCollection, _, target, getOptions))

  def insert[T](asyncCollection: AsyncCollection,
      insertOptions: InsertOptions = InsertOptions.insertOptions())
      : Flow[MutationDocument[T], MutationDocument[T], NotUsed] = {
    Flow[MutationDocument[T]]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.insert(doc.id, doc.content, insertOptions))
          // todo: recovery if failed
          .map(doc.withResult)
      }
  }

  def insert[T](asyncCollection: AsyncCollection, applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions()): Flow[T, T, NotUsed] = {
    Flow[T]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.insert(applyId(doc), doc, insertOptions))
          .map(_ => doc)
      }
  }

  def replace[T](asyncCollection: AsyncCollection,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())
      : Flow[MutationDocument[T], MutationDocument[T], NotUsed] = {
    Flow[MutationDocument[T]]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.replace(doc.id, doc.content, replaceOptions))
          .map(doc.withResult)
      }
  }

  def replace[T](asyncCollection: AsyncCollection, applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions()): Flow[T, T, NotUsed] = {
    Flow[T]
      .flatMapConcat { content =>
        Source.completionStage(asyncCollection.replace(applyId(content), content, replaceOptions))
          .map(_ => content)
      }
  }

  def upsert[T](asyncCollection: AsyncCollection,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())
      : Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    Flow[MutationDocument[T]]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.upsert(doc.id, doc.content, upsertOptions))
          .map(doc.withResult)
      }

  def upsert[T](asyncCollection: AsyncCollection, applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Flow[T, T, NotUsed] =
    Flow[T]
      .flatMapConcat { content =>
        Source.completionStage(asyncCollection.upsert(applyId(content), content, upsertOptions))
          .map(_ => content)
      }

  def remove(asyncCollection: AsyncCollection,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions()): Flow[String, String, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source
          .completionStage(asyncCollection.remove(id, removeOptions))
          .map(_ => id)
      }
  }

  def exists(asyncCollection: AsyncCollection,
      existsOptions: ExistsOptions = ExistsOptions.existsOptions()): Flow[String, Boolean, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.exists(id, existsOptions))
          .map(_.exists())
      }
  }

  def mutateIn(asyncCollection: AsyncCollection, specs: java.util.List[MutateInSpec], options: MutateInOptions)
      : Flow[String, MutateInResult, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.mutateIn(id, specs, options))
      }
  }

  def touch(asyncCollection: AsyncCollection, expiry: Duration,
      touchOptions: TouchOptions): Flow[String, String, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.touch(id, expiry, touchOptions))
          .map(_ => id)
      }
  }

  def touch(asyncCollection: AsyncCollection, expiry: Instant,
      touchOptions: TouchOptions): Flow[String, String, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.touch(id, expiry, touchOptions))
          .map(_ => id)
      }
  }

  def append(asyncCollection: AsyncCollection,
      options: AppendOptions = AppendOptions.appendOptions()): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    Flow[MutationBinaryDocument]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.binary().append(doc.id, doc.content, options))
      }

  def prepend(asyncCollection: AsyncCollection,
      options: PrependOptions = PrependOptions.prependOptions())
      : Flow[MutationBinaryDocument, MutationResult, NotUsed] = {
    Flow[MutationBinaryDocument]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.binary().prepend(doc.id, doc.content, options))

      }
  }

  def increment(asyncCollection: AsyncCollection,
      options: IncrementOptions): Flow[String, CounterResult, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.binary().increment(id, options))
      }
  }

  def decrement(asyncCollection: AsyncCollection,
      options: DecrementOptions): Flow[String, CounterResult, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.binary().decrement(id, options))
      }
  }

}
