/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.couchbase.scaladsl

import com.couchbase.client.java._
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.QueryIndex
import com.couchbase.client.java.query.QueryResult
import org.apache.pekko
import org.apache.pekko.annotation.{ DoNotInherit, InternalApi }
import org.apache.pekko.stream.connectors.couchbase.{ CouchbaseMutationResult, CouchbaseSessionSettings }
import org.apache.pekko.stream.connectors.couchbase.impl.CouchbaseSessionImpl
import org.apache.pekko.stream.connectors.couchbase.javadsl.{ CouchbaseSession => JavaDslCouchbaseSession }
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.{ Done, NotUsed }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Scala API: Gives access to Couchbase.
 *
 * @see [[pekko.stream.connectors.couchbase.CouchbaseSessionRegistry]]
 */
object CouchbaseSession {

  def apply(settings: CouchbaseSessionSettings, bucketName: String)(
      implicit ec: ExecutionContext): Future[CouchbaseSession] =
    createClusterClient(settings).map(c => create(c, bucketName, None, None))

  def apply(settings: CouchbaseSessionSettings, bucketName: String, collection: String)(
      implicit ec: ExecutionContext): Future[CouchbaseSession] =
    createClusterClient(settings).map(c => create(c, bucketName, None, Some(collection)))

  /**
   * Create a session against the given bucket. The couchbase client used to connect will be created and then closed when
   * the session is closed.
   */
  def apply(settings: CouchbaseSessionSettings, bucketName: String, scope: String, collection: String)(
      implicit ec: ExecutionContext): Future[CouchbaseSession] =
    createClusterClient(settings).map(c => create(c, bucketName, Some(scope), Some(collection)))

  /**
   * Create a given bucket using a pre-existing cluster client, allowing for it to be shared among
   * multiple `CouchbaseSession`s. The cluster client's life-cycle is the user's responsibility.
   */
  def apply(cluster: AsyncCluster, bucketName: String, scope: Option[String] = Option.empty,
      collection: Option[String] = Option.empty)(implicit ec: ExecutionContext): Future[CouchbaseSession] =
    Future(create(cluster, bucketName, scope, collection))

  /**
   * Create a given bucket using a pre-existing cluster client, allowing for it to be shared among
   * multiple `CouchbaseSession`s. The cluster client's life-cycle is the user's responsibility.
   */
  @InternalApi
  private[couchbase] def create(cluster: AsyncCluster, bucketName: String, scopeName: Option[String],
      collectionName: Option[String]): CouchbaseSession = {
    val bucket = cluster.bucket(bucketName)
    val scope = scopeName.map(bucket.scope).getOrElse(bucket.defaultScope())
    val collection = collectionName.map(scope.collection).get
    new CouchbaseSessionImpl(cluster, bucket, scope, collection).asScala
  }

  /**
   * INTERNAL API.
   *
   * Connects to a Couchbase cluster by creating an `AsyncCluster`.
   * The life-cycle of it is the user's responsibility.
   */
  @InternalApi
  private[couchbase] def createClusterClient(settings: CouchbaseSessionSettings)(
      implicit ec: ExecutionContext): Future[AsyncCluster] =
    settings.enriched
      .flatMap { enrichedSettings =>
        Future(enrichedSettings.environment match {
          case Some(environment) =>
            ClusterOptions
              .clusterOptions(enrichedSettings.username, enrichedSettings.username)
              .environment(environment)
          case None =>
            ClusterOptions.clusterOptions(enrichedSettings.username, enrichedSettings.username)
        }).map { clusterOptions =>
          Cluster.connect(enrichedSettings.nodes.mkString(","), clusterOptions).async()
        }
      }

}

/**
 * Scala API: A Couchbase session allowing querying and interacting with a specific couchbase bucket.
 *
 * Not for user extension.
 */
@DoNotInherit
trait CouchbaseSession {

  def underlying: AsyncCluster

  def asJava: JavaDslCouchbaseSession

  /**
   * Insert a JSON document using the given write settings.
   *
   * For inserting other types of documents see `insertDoc`.
   */
  def insert(id: String, document: Object, insertOptions: InsertOptions = InsertOptions.insertOptions())
      : Future[MutationResult]

  /**
   * @param getOptions Query configuration, such as configuring timeout time
   * @return A document if found or none if there is no document for the id
   */
  def get(id: String, getOptions: GetOptions = GetOptions.getOptions): Future[GetResult]

  /**
   * @return A document of the given type if found or none if there is no document for the id
   */
  def get[T](id: String, documentClass: Class[T], getOptions: GetOptions = GetOptions.getOptions): Future[T]

  /**
   * Upsert using the given write settings
   *
   * Separate from `upsert` to make the most common case smoother with the type inference
   *
   * @return a future that completes when the upsert is done
   */
  def upsert[T](
      id: String, document: T, upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Future[MutationResult]

  /**
   * Replace using the given write settings
   *
   * Separate from `replace` to make the most common case smoother with the type inference
   *
   * @return a future that completes when the replace is done
   */
  def replace[T](id: String, document: T, replaceOptions: ReplaceOptions): Future[MutationResult]

  /**
   * Remove a document by id using the default write settings.
   *
   * @return Future that completes when the document has been removed, if there is no such document
   *         the future is failed with a `DocumentDoesNotExistException`
   */
  def remove(id: String, removeOptions: RemoveOptions = RemoveOptions.removeOptions()): Future[MutationResult]

  def streamedQuery(query: String): Source[QueryResult, NotUsed]

  /**
   * Create or increment a counter
   *
   * @param id What counter document id
   * @return The value of the counter after applying the delta
   */
  def counter(
      id: String, incrementOptions: IncrementOptions = IncrementOptions.incrementOptions()): Future[CounterResult]

  /**
   * Close the session and release all resources it holds. Subsequent calls to other methods will likely fail.
   */
  def close(): Future[Done]

  /**
   * Create a secondary index for the current bucket.
   *
   * @param indexName     the name of the index.
   * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
   *                      is set to true.
   * @param fields        the JSON fields to index - each can be either `String` or [com.couchbase.client.java.query.dsl.Expression]
   * @return a [[scala.concurrent.Future]] of `true` if the index was/will be effectively created, `false`
   *         if the index existed and `ignoreIfExist` is `true`. Completion of the future does not guarantee the index is online
   *         and ready to be used.
   */
  def createIndex(indexName: String, ignoreIfExist: Boolean, fields: String*): Future[Boolean]

  /**
   * List the existing secondary indexes for the bucket
   */
  def listIndexes(): Source[List[QueryIndex], NotUsed]
}
