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
import com.couchbase.client.java.kv.{ InsertOptions, RemoveOptions, ReplaceOptions, UpsertOptions }
import org.apache.pekko.stream.connectors.couchbase3.MutationDocument
import org.apache.pekko.stream.scaladsl.{ Keep, Sink }
import org.apache.pekko.Done
import scala.concurrent.Future

object CouchbaseSink {

  def insert(asyncCollection: AsyncCollection,
      insertOptions: InsertOptions = InsertOptions.insertOptions()): Sink[MutationDocument[Nothing], Future[Done]] = {
    CouchbaseFlow.insert(asyncCollection, insertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def insert[T](asyncCollection: AsyncCollection, applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions()): Sink[T, Future[Done]] = {
    CouchbaseFlow.insert(asyncCollection, applyId, insertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def upsert(asyncCollection: AsyncCollection,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Sink[MutationDocument[Nothing], Future[Done]] = {
    CouchbaseFlow.upsert(asyncCollection, upsertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def upsert[T](asyncCollection: AsyncCollection, applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Sink[T, Future[Done]] = {
    CouchbaseFlow.upsert(asyncCollection, applyId, upsertOptions).toMat(Sink.ignore)(Keep.right)
  }

  def replace(asyncCollection: AsyncCollection,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())
      : Sink[MutationDocument[Nothing], Future[Done]] = {
    CouchbaseFlow.replace(asyncCollection, replaceOptions).toMat(Sink.ignore)(Keep.right)
  }

  def replace[T](asyncCollection: AsyncCollection, applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions()): Sink[T, Future[Done]] = {
    CouchbaseFlow.replace(asyncCollection, applyId, replaceOptions).toMat(Sink.ignore)(Keep.right)
  }

  def remove(asyncCollection: AsyncCollection,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions()): Sink[String, Future[Done]] = {
    CouchbaseFlow.remove(asyncCollection, removeOptions).toMat(Sink.ignore)(Keep.right)
  }

}
