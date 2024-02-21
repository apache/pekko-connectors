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

package org.apache.pekko.stream.connectors.couchbase.javadsl

import com.couchbase.client.java.AsyncCluster
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.QueryIndex
import com.couchbase.client.java.query.QueryResult
import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.annotation.DoNotInherit
import pekko.stream.connectors.couchbase.CouchbaseSessionSettings
import pekko.stream.connectors.couchbase.scaladsl.{ CouchbaseSession => ScalaDslCouchbaseSession }
import pekko.stream.javadsl.Source
import pekko.util.FutureConverters._

import java.util.concurrent.{ CompletionStage, Executor }
import scala.concurrent.ExecutionContext

/**
 * Java API: Gives access to Couchbase.
 *
 * @see [[pekko.stream.connectors.couchbase.CouchbaseSessionRegistry]]
 */
object CouchbaseSession {

  /**
   * Create a session against the given bucket. The couchbase client used to connect will be created and then closed when
   * the session is closed.
   */
  def create(settings: CouchbaseSessionSettings,
      bucketName: String,
      executor: Executor): CompletionStage[CouchbaseSession] =
    ScalaDslCouchbaseSession
      .apply(settings, bucketName)(executionContext(executor))
      .map(_.asJava)(executionContext(executor))
      .asJava

  /**
   * Connects to a Couchbase cluster by creating an `AsyncCluster`.
   * The life-cycle of it is the user's responsibility.
   */
  def createClient(settings: CouchbaseSessionSettings, executor: Executor): CompletionStage[AsyncCluster] =
    ScalaDslCouchbaseSession
      .createClusterClient(settings)(executionContext(executor))
      .asJava

  private def executionContext(executor: Executor): ExecutionContext =
    executor match {
      case ec: ExecutionContext => ec
      case _                    => ExecutionContext.fromExecutor(executor)
    }

}

/**
 * Java API: A Couchbase session allowing querying and interacting with a specific couchbase bucket.
 *
 * Not for user extension.
 */
// must be an abstract class, otherwise static forwarders are missing for companion object if building with Scala 2.11
@DoNotInherit
abstract class CouchbaseSession {

  def underlying: AsyncCluster

  def asScala: ScalaDslCouchbaseSession

  /**
   * Insert a JSON document using the given write settings.
   *
   * For inserting other types of documents see `insertDoc`.
   */
  def insert(id: String, document: Object, insertOptions: InsertOptions): CompletionStage[MutationResult]

  /**
   * @return A document if found or none if there is no document for the id
   */
  def get(id: String, getOptions: GetOptions): CompletionStage[GetResult]

  /**
   * @return A document if found or none if there is no document for the id
   */
  def get[T](id: String, documentClass: Class[T], getOptions: GetOptions): CompletionStage[T]

  /**
   * Upsert using the given write settings.
   *
   * For inserting other types of documents see `upsertDoc`.
   *
   * @return a CompletionStage that completes when the upsert is done
   */
  def upsert[T](id: String, document: T, upsertOptions: UpsertOptions): CompletionStage[MutationResult]

  /**
   * Replace using the default write settings
   *
   * For replacing other types of documents see `replaceDoc`.
   *
   * @return a CompletionStage that completes when the replace is done
   */
  def replace[T](id: String, document: T, replaceOptions: ReplaceOptions): CompletionStage[MutationResult]

  /**
   * Remove a document by id using the default write settings.
   *
   * @return CompletionStage that completes when the document has been removed, if there is no such document
   *         the CompletionStage is failed with a `DocumentDoesNotExistException`
   */
  def remove(id: String, removeOptions: RemoveOptions): CompletionStage[MutationResult]

  def streamedQuery(query: String): Source[QueryResult, NotUsed]

  /**
   * Create or increment a counter
   *
   * @param id What counter document id
   * @return The value of the counter after applying the delta
   */
  def counter(id: String, incrementOptions: IncrementOptions): CompletionStage[CounterResult]

  /**
   * Close the session and release all resources it holds. Subsequent calls to other methods will likely fail.
   */
  def close(): CompletionStage[Done]

  /**
   * Create a secondary index for the current bucket.
   *
   * @param indexName     the name of the index.
   * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
   *                      is set to true.
   * @param fields        the JSON fields to index - each can be either `String` or [[com.couchbase.client.java.query.dsl.Expression]]
   * @return a [[java.util.concurrent.CompletionStage]] of `true` if the index was/will be effectively created, `false`
   *         if the index existed and ignoreIfExist` is true. Completion of the `CompletionStage` does not guarantee the index
   *         is online and ready to be used.
   */
  def createIndex(indexName: String, ignoreIfExist: Boolean, fields: String*): CompletionStage[Boolean]

  /**
   * List the existing secondary indexes for the bucket
   */
  def listIndexes(): Source[java.util.List[QueryIndex], NotUsed]
}
