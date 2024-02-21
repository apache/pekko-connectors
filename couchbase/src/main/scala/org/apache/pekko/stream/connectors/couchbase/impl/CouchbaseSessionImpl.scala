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

import com.couchbase.client.java._
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{CreateQueryIndexOptions, QueryIndex}
import com.couchbase.client.java.query.QueryResult
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.couchbase.javadsl.CouchbaseSession
import org.apache.pekko.stream.connectors.couchbase.scaladsl
import org.apache.pekko.stream.javadsl.Source
import org.apache.pekko.util.FutureConverters.CompletionStageOps

import java.util.concurrent.CompletionStage
import scala.jdk.CollectionConverters.{IterableHasAsScala, SeqHasAsJava}
import scala.language.implicitConversions

/**
 * INTERNAL API
 */
@InternalApi
final private[couchbase] class CouchbaseSessionImpl(cluster: AsyncCluster,
    bucket: AsyncBucket,
    scope: AsyncScope,
    collection: AsyncCollection)
    extends CouchbaseSession {
  override def asScala: scaladsl.CouchbaseSession = new CouchbaseSessionScalaAdapter(this)

  override def underlying = cluster

  override def insert(id: String, document: Object, insertOptions: InsertOptions): CompletionStage[MutationResult] = {
    collection.insert(id, document, insertOptions)
  }

  override def get(id: String, getOptions: GetOptions): CompletionStage[GetResult] = {
    collection.get(id, getOptions)
  }

  override def get[T](id: String, documentClass: Class[T], getOptions: GetOptions): CompletionStage[T] =
    collection.get(id, getOptions).thenApply(_.contentAs(documentClass))

  override def upsert[T](id: String, document: T, upsertOptions: UpsertOptions): CompletionStage[MutationResult] =
    collection.upsert(id, document, upsertOptions)

  override def replace[T](id: String, document: T, replaceOptions: ReplaceOptions): CompletionStage[MutationResult] =
    collection.replace(id, document, replaceOptions)

  override def remove(id: String, removeOptions: RemoveOptions): CompletionStage[MutationResult] = {
    collection.remove(id, removeOptions)
  }

  override def streamedQuery(query: String): Source[QueryResult, NotUsed] =
    Source.future(cluster.query(query).asScala)

  override def counter(id: String, incrementOptions: IncrementOptions): CompletionStage[CounterResult] = {
    collection.binary().increment(id, incrementOptions)
  }

  override def close(): CompletionStage[Done] =
    cluster.disconnect().thenApply(_ => Done.done())

  override def createIndex(indexName: String, ignoreIfExist: Boolean, fields: String*): CompletionStage[Boolean] = {
    val options = CreateQueryIndexOptions.createQueryIndexOptions()
      .ignoreIfExists(ignoreIfExist)
    val fieldsAsJava = fields.toList.asJava
    cluster.queryIndexes().createIndex(bucket.name(), indexName, fieldsAsJava, options).thenApply(_ => true)
  }

  override def listIndexes(): Source[java.util.List[QueryIndex], NotUsed] =
    Source.future(cluster.queryIndexes().getAllIndexes(bucket.name()).asScala)
}
