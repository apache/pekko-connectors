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
import com.couchbase.client.java.kv.{ ExistsOptions, InsertOptions, RemoveOptions, ReplaceOptions, UpsertOptions }
import org.apache.pekko.stream.connectors.couchbase3.MutationDocument
import org.apache.pekko.stream.scaladsl.{ Keep, Sink }
import org.apache.pekko.Done

import scala.concurrent.Future

object CouchbaseSink {

  def insertDoc[T](insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Sink[MutationDocument[T], Future[Done]] = {
    CouchbaseFlow.insertDoc[T](insertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def insert[T](applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] = {
    CouchbaseFlow.insert[T](applyId, insertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def upsertDoc[T](upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Sink[MutationDocument[T], Future[Done]] = {
    CouchbaseFlow.upsertDoc[T](upsertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def upsert[T](applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] = {
    CouchbaseFlow.upsert[T](applyId, upsertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def replaceDoc[T](
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Sink[MutationDocument[T], Future[Done]] = {
    CouchbaseFlow.replaceDoc[T](replaceOptions).toMat(Sink.ignore)(Keep.right)
  }

  def replace[T](applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] = {
    CouchbaseFlow.replace[T](applyId, replaceOptions).toMat(Sink.ignore)(Keep.right)
  }

  def remove[T](applyId: T => String,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] = {
    CouchbaseFlow.remove(applyId, removeOptions).toMat(Sink.ignore)(Keep.right)
  }

  def exists[T](applyId: T => String, existsOptions: ExistsOptions = ExistsOptions.existsOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Boolean]] =
    CouchbaseFlow.exists(applyId, existsOptions).toMat(Sink.head)(Keep.right)

}
