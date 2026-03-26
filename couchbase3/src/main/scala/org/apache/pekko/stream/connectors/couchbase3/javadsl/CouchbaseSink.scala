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
import com.couchbase.client.java.kv._
import org.apache.pekko.stream.connectors.couchbase3.MutationDocument
import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.{ CouchbaseSink => ScalaCouchbaseSink }
import org.apache.pekko.stream.javadsl.Sink

import java.util.concurrent.CompletionStage
import java.util.function.{ Function => JFunction }
import scala.concurrent.Future
import scala.jdk.FutureConverters._

object CouchbaseSink {

  /**
   * reference to [[CouchbaseFlow.insertDoc]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #insertDocFuture} which works like this method worked in 1.x.
   */
  def insertDoc[T](insertOptions: InsertOptions,
      asyncCollection: AsyncCollection): Sink[MutationDocument[T], CompletionStage[Done]] =
    ScalaCouchbaseSink.insertDoc[T](insertOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.insertDoc]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #insertDocFuture} which works like this method worked in 1.x.
   */
  def insertDoc[T](
      asyncCollection: AsyncCollection): Sink[MutationDocument[T], CompletionStage[Done]] =
    ScalaCouchbaseSink.insertDoc[T](InsertOptions.insertOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.insertDoc]]
   * @deprecated Use insertDoc which returns CompletionStage instead
   */
  @deprecated("Use insertDoc which returns CompletionStage instead", since = "2.0.0")
  def insertDocFuture[T](insertOptions: InsertOptions)(
      implicit asyncCollection: AsyncCollection): Sink[MutationDocument[T], Future[Done]] =
    ScalaCouchbaseSink.insertDoc[T](insertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.insert]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #insertFuture} which works like this method worked in 1.x.
   */
  def insert[T](applyId: JFunction[T, String],
      insertOptions: InsertOptions,
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.insert[T](applyId.apply, insertOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.insert]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #insertFuture} which works like this method worked in 1.x.
   */
  def insert[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.insert[T](applyId.apply, InsertOptions.insertOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.insert]]
   * @deprecated Use insert which returns CompletionStage instead
   */
  @deprecated("Use insert which returns CompletionStage instead", since = "2.0.0")
  def insertFuture[T](applyId: T => String,
      insertOptions: InsertOptions)(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] =
    ScalaCouchbaseSink.insert[T](applyId, insertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.upsertDoc]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #upsertDocFuture} which works like this method worked in 1.x.
   */
  def upsertDoc[T](upsertOptions: UpsertOptions,
      asyncCollection: AsyncCollection): Sink[MutationDocument[T], CompletionStage[Done]] =
    ScalaCouchbaseSink.upsertDoc[T](upsertOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.upsertDoc]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #upsertDocFuture} which works like this method worked in 1.x.
   */
  def upsertDoc[T](
      asyncCollection: AsyncCollection): Sink[MutationDocument[T], CompletionStage[Done]] =
    ScalaCouchbaseSink.upsertDoc[T](UpsertOptions.upsertOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.upsertDoc]]
   * @deprecated Use upsertDoc which returns CompletionStage instead
   */
  @deprecated("Use upsertDoc which returns CompletionStage instead", since = "2.0.0")
  def upsertDocFuture[T](upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Sink[MutationDocument[T], Future[Done]] =
    ScalaCouchbaseSink.upsertDoc[T](upsertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.upsert]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #upsertFuture} which works like this method worked in 1.x.
   */
  def upsert[T](applyId: JFunction[T, String],
      upsertOptions: UpsertOptions,
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.upsert[T](applyId.apply, upsertOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.upsert]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #upsertFuture} which works like this method worked in 1.x.
   */
  def upsert[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.upsert[T](applyId.apply, UpsertOptions.upsertOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.upsert]]
   * @deprecated Use upsert which returns CompletionStage instead
   */
  @deprecated("Use upsert which returns CompletionStage instead", since = "2.0.0")
  def upsertFuture[T](applyId: T => String,
      upsertOptions: UpsertOptions = UpsertOptions.upsertOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] =
    ScalaCouchbaseSink.upsert[T](applyId, upsertOptions).asJava

  /**
   * reference to [[CouchbaseFlow.replaceDoc]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #replaceDocFuture} which works like this method worked in 1.x.
   */
  def replaceDoc[T](
      replaceOptions: ReplaceOptions,
      asyncCollection: AsyncCollection): Sink[MutationDocument[T], CompletionStage[Done]] =
    ScalaCouchbaseSink.replaceDoc[T](replaceOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.replaceDoc]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #replaceDocFuture} which works like this method worked in 1.x.
   */
  def replaceDoc[T](
      asyncCollection: AsyncCollection): Sink[MutationDocument[T], CompletionStage[Done]] =
    ScalaCouchbaseSink.replaceDoc[T](ReplaceOptions.replaceOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.replaceDoc]]
   * @deprecated Use replaceDoc which returns CompletionStage instead
   */
  @deprecated("Use replaceDoc which returns CompletionStage instead", since = "2.0.0")
  def replaceDocFuture[T](
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Sink[MutationDocument[T], Future[Done]] =
    ScalaCouchbaseSink.replaceDoc[T](replaceOptions).asJava

  /**
   * reference to [[CouchbaseFlow.replace]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #replaceFuture} which works like this method worked in 1.x.
   */
  def replace[T](applyId: JFunction[T, String],
      replaceOptions: ReplaceOptions,
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.replace[T](applyId.apply, replaceOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.replace]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #replaceFuture} which works like this method worked in 1.x.
   */
  def replace[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.replace[T](applyId.apply, ReplaceOptions.replaceOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.replace]]
   * @deprecated Use replace which returns CompletionStage instead
   */
  @deprecated("Use replace which returns CompletionStage instead", since = "2.0.0")
  def replaceFuture[T](applyId: T => String,
      replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] =
    ScalaCouchbaseSink.replace[T](applyId, replaceOptions).asJava

  /**
   * reference to [[CouchbaseFlow.remove]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #removeFuture} which works like this method worked in 1.x.
   */
  def remove[T](applyId: JFunction[T, String],
      removeOptions: RemoveOptions,
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.remove[T](applyId.apply, removeOptions)(asyncCollection).mapMaterializedValue(_.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.remove]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #removeFuture} which works like this method worked in 1.x.
   */
  def remove[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[Done]] =
    ScalaCouchbaseSink.remove[T](applyId.apply, RemoveOptions.removeOptions())(asyncCollection).mapMaterializedValue(
      _.asJava).asJava

  /**
   * reference to [[CouchbaseFlow.remove]]
   * @deprecated Use remove which returns CompletionStage instead
   */
  @deprecated("Use remove which returns CompletionStage instead", since = "2.0.0")
  def removeFuture[T](applyId: T => String,
      removeOptions: RemoveOptions = RemoveOptions.removeOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Done]] =
    ScalaCouchbaseSink.remove[T](applyId, removeOptions).asJava

  /**
   * reference to [[CouchbaseFlow.exists]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #existsFuture} which works like this method worked in 1.x.
   */
  def exists[T](applyId: JFunction[T, String], existsOptions: ExistsOptions,
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[java.lang.Boolean]] =
    ScalaCouchbaseSink.exists[T](applyId.apply, existsOptions)(asyncCollection)
      .mapMaterializedValue(_.map(Boolean.box)(scala.concurrent.ExecutionContext.parasitic).asJava)
      .asJava

  /**
   * reference to [[CouchbaseFlow.exists]]
   * <p>
   *   This function's return type changed in 2.0.0 to return a Sink with a CompletionStage instead of a
   *   Scala Future, to be more consistent with Java usage.
   * </p>
   * @see {@link #existsFuture} which works like this method worked in 1.x.
   */
  def exists[T](applyId: JFunction[T, String],
      asyncCollection: AsyncCollection): Sink[T, CompletionStage[java.lang.Boolean]] =
    ScalaCouchbaseSink.exists[T](applyId.apply, ExistsOptions.existsOptions())(asyncCollection)
      .mapMaterializedValue(_.map(Boolean.box)(scala.concurrent.ExecutionContext.parasitic).asJava)
      .asJava

  /**
   * reference to [[CouchbaseFlow.exists]]
   * @deprecated Use exists which returns CompletionStage instead
   */
  @deprecated("Use exists which returns CompletionStage instead", since = "2.0.0")
  def existsFuture[T](applyId: T => String, existsOptions: ExistsOptions = ExistsOptions.existsOptions())(
      implicit asyncCollection: AsyncCollection): Sink[T, Future[Boolean]] =
    ScalaCouchbaseSink.exists[T](applyId, existsOptions).asJava

}
