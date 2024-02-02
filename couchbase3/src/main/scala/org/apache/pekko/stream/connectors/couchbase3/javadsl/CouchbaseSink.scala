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
import com.couchbase.client.java.kv.{ InsertOptions, RemoveOptions, ReplaceOptions, UpsertOptions }
import org.apache.pekko.stream.connectors.couchbase3.MutationDocument
import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.couchbase3.scaladsl
import org.apache.pekko.stream.javadsl.Sink

import scala.concurrent.Future

object CouchbaseSink {

  def insert(asyncCollection: AsyncCollection,
      insertOptions: InsertOptions = InsertOptions.insertOptions()): Sink[MutationDocument[Nothing], Future[Done]] =
    scaladsl.CouchbaseSink.insert(asyncCollection, insertOptions).asJava

  def insert[T](asyncCollection: AsyncCollection, applyId: T => String,
      insertOptions: InsertOptions = InsertOptions.insertOptions()): Sink[T, Future[Done]] =
    scaladsl.CouchbaseSink.insert(asyncCollection, applyId, insertOptions).asJava

  def upsert(asyncCollection: AsyncCollection,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Sink[MutationDocument[Nothing], Future[Done]] =
    scaladsl.CouchbaseSink.upsert(asyncCollection, upsertOptions).asJava

  def upsert[T](asyncCollection: AsyncCollection, applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Sink[T, Future[Done]] =
    scaladsl.CouchbaseSink.upsert(asyncCollection, applyId, upsertOptions).asJava

  def replace(asyncCollection: AsyncCollection,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions()): Sink[MutationDocument[Nothing], Future[Done]] =
    scaladsl.CouchbaseSink.replace(asyncCollection, replaceOptions).asJava

  def replace[T](asyncCollection: AsyncCollection, applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions()): Sink[T, Future[Done]] =
    scaladsl.CouchbaseSink.replace(asyncCollection, applyId, replaceOptions).asJava

  def remove(asyncCollection: AsyncCollection,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions()): Sink[String, Future[Done]] =
    scaladsl.CouchbaseSink.remove(asyncCollection, removeOptions).asJava

}
