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

package org.apache.pekko.stream.connectors.couchbase.impl

import com.couchbase.client.java.AsyncCluster
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.QueryIndex
import com.couchbase.client.java.query.QueryResult
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.couchbase.{ javadsl, scaladsl }

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.FutureConverters._

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * INTERNAL API
 */
@InternalApi
private[couchbase] final class CouchbaseSessionScalaAdapter(delegate: javadsl.CouchbaseSession)
    extends scaladsl.CouchbaseSession {

  override def asJava: javadsl.CouchbaseSession = delegate

  override def underlying: AsyncCluster = delegate.underlying

  /**
   * Insert a JSON document using the given write settings.
   *
   * For inserting other types of documents see `insertDoc`.
   */
  override def insert(id: String, document: Object, insertOptions: InsertOptions = InsertOptions.insertOptions()): Future[MutationResult] =
    delegate.insert(id, document, insertOptions).asScala

  /**
   * @return A document if found or none if there is no document for the id
   */
  override def get(id: String, getOptions: GetOptions): Future[GetResult] =
    delegate.get(id, getOptions).asScala

  /**
   * @return A document if found or none if there is no document for the id
   */
  override def get[T](id: String, documentClass: Class[T], getOptions: GetOptions = GetOptions.getOptions): Future[T] =
    delegate.get(id, documentClass, getOptions).asScala

  /**
   * Upsert using the given write settings.
   *
   * For inserting other types of documents see `upsertDoc`.
   *
   * @return a Future that completes when the upsert is done
   */
  override def upsert[T](id: String, document: T, upsertOptions: UpsertOptions = UpsertOptions.upsertOptions()): Future[MutationResult] =
    delegate.upsert(id, document, upsertOptions).asScala

  /**
   * Replace using the default write settings
   *
   * For replacing other types of documents see `replaceDoc`.
   *
   * @return a Future that completes when the replace is done
   */
  override def replace[T](id: String, document: T, replaceOptions: ReplaceOptions = ReplaceOptions.replaceOptions()): Future[MutationResult] =
    delegate.replace(id, document, replaceOptions).asScala

  /**
   * Remove a document by id using the default write settings.
   *
   * @return Future that completes when the document has been removed, if there is no such document
   *         the Future is failed with a `DocumentDoesNotExistException`
   */
  override def remove(id: String, removeOptions: RemoveOptions = RemoveOptions.removeOptions())
  : Future[MutationResult] =
    delegate.remove(id, removeOptions).asScala

  override def streamedQuery(query: String): Source[QueryResult, NotUsed] =
    delegate.streamedQuery(query).asScala

  /**
   * Create or increment a counter
   *
   * @param id What counter document id
   * @return The value of the counter after applying the delta
   */
  override def counter(id: String, incrementOptions: IncrementOptions = IncrementOptions.incrementOptions())
  : Future[CounterResult] =
    delegate.counter(id, incrementOptions).asScala

  /**
   * Close the session and release all resources it holds. Subsequent calls to other methods will likely fail.
   */
  override def close(): Future[Done] =
    delegate.close().asScala

  /**
   * Create a secondary index for the current bucket.
   *
   * @param indexName     the name of the index.
   * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
   *                      is set to true.
   * @param fields        the JSON fields to index
   * @return a [[java.util.concurrent.Future]] of `true` if the index was/will be effectively created, `false`
   *         if the index existed and ignoreIfExist` is true. Completion of the `Future` does not guarantee the index
   *         is online and ready to be used.
   */
  override def createIndex(indexName: String, ignoreIfExist: Boolean, fields: String*): Future[Boolean] =
    delegate.createIndex(indexName, ignoreIfExist, fields: _*).asScala

  /**
   * List the existing secondary indexes for the bucket
   */
  override def listIndexes(): Source[List[QueryIndex], NotUsed] =
    delegate.listIndexes().asScala.map(_.asScala.toList)
}
