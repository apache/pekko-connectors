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
import com.couchbase.client.java.kv._
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.couchbase3.{MutationBinaryDocument, MutationDocument}
import org.apache.pekko.stream.scaladsl.{Flow, Source}

import java.time.Duration

object CouchbaseFlow {

  def get(getOptions: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, GetResult, NotUsed] =
    Flow[String].flatMapConcat(CouchbaseSource.get(_, getOptions))

  def getClass[T](target: Class[T], getOptions: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getClass(_, target, getOptions))

  def getType[T](target: TypeRef[T], getOptions: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getType(_, target, getOptions))

  def getAllReplicas(getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, GetReplicaResult, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getAllReplicas(_, getOptions))

  def getAllReplicasClass[T](target: Class[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getAllReplicasClass(_, target, getOptions))

  def getAllReplicasType[T](target: TypeRef[T],
      getOptions: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, T, NotUsed] =
    Flow[String]
      .flatMapConcat(CouchbaseSource.getAllReplicasType(_, target, getOptions))

  def insert[T](applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] = {
    Flow[T]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.insert(applyId(doc), doc, insertOptions))
          .map(_ => doc)
      }
  }

  def insertDoc[T](insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    Flow[MutationDocument[T]]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.insert(doc.id, doc.content, insertOptions))
          .map(doc.withResult)
      }

  def replace[T](applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    Flow[T]
      .flatMapConcat { content =>
        Source.completionStage(asyncCollection.replace(applyId(content), content, replaceOptions))
          .map(_ => content)
      }

  def replaceDoc[T](replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    Flow[MutationDocument[T]]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.replace(doc.id, doc.content, replaceOptions))
          .map(doc.withResult)
      }

  def upsert[T](applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[T, T, NotUsed] =
    Flow[T]
      .flatMapConcat { content =>
        Source.completionStage(asyncCollection.upsert(applyId(content), content, upsertOptions))
          .map(_ => content)
      }

  def upsertDoc[T](
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationDocument[T], MutationDocument[T], NotUsed] =
    Flow[MutationDocument[T]]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.upsert(doc.id, doc.content, upsertOptions))
          .map(doc.withResult)
      }

  def remove(removeOptions: RemoveOptions = RemoveOptions.removeOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, String, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source
          .completionStage(asyncCollection.remove(id, removeOptions))
          .map(_ => id)
      }
  }

  def exists(
      existsOptions: ExistsOptions = ExistsOptions.existsOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, Boolean, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.exists(id, existsOptions))
          .map(_.exists())
      }
  }

  def mutateIn(specs: java.util.List[MutateInSpec], options: MutateInOptions = MutateInOptions.mutateInOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, MutateInResult, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.mutateIn(id, specs, options))
      }
  }

  def touch(expiry: Duration, touchOptions: TouchOptions = TouchOptions.touchOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, String, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.touch(id, expiry, touchOptions))
          .map(_ => id)
      }
  }

  def append(options: AppendOptions = AppendOptions.appendOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] =
    Flow[MutationBinaryDocument]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.binary().append(doc.id, doc.content, options))
      }

  def prepend(options: PrependOptions = PrependOptions.prependOptions())(
      implicit asyncCollection: AsyncCollection): Flow[MutationBinaryDocument, MutationResult, NotUsed] = {
    Flow[MutationBinaryDocument]
      .flatMapConcat { doc =>
        Source.completionStage(asyncCollection.binary().prepend(doc.id, doc.content, options))

      }
  }

  def increment(options: IncrementOptions = IncrementOptions.incrementOptions())(
      implicit asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.binary().increment(id, options))
      }
  }

  def decrement(options: DecrementOptions)(
      implicit asyncCollection: AsyncCollection): Flow[String, CounterResult, NotUsed] = {
    Flow[String]
      .flatMapConcat { id =>
        Source.completionStage(asyncCollection.binary().decrement(id, options))
      }
  }

}
