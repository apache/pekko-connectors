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

import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletionStage
import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.dispatch.ExecutionContexts
import pekko.stream.connectors.couchbase.CouchbaseWriteSettings
import pekko.stream.connectors.couchbase.javadsl
import pekko.stream.connectors.couchbase.scaladsl
import pekko.stream.javadsl.Source
import pekko.{ Done, NotUsed }
import pekko.util.FutureConverters._
import pekko.util.OptionConverters._
import com.couchbase.client.java.AsyncBucket
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.document.{ Document, JsonDocument }
import com.couchbase.client.java.query.util.IndexInfo
import com.couchbase.client.java.query.{ N1qlQuery, Statement }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ duration, Future }

/**
 * INTERNAL API
 */
@InternalApi
private[couchbase] final class CouchbaseSessionJavaAdapter(delegate: scaladsl.CouchbaseSession)
    extends javadsl.CouchbaseSession {

  override def asScala: scaladsl.CouchbaseSession = delegate

  override def underlying: AsyncBucket = delegate.underlying

  override def insert(document: JsonDocument): CompletionStage[JsonDocument] = delegate.insertDoc(document).asJava

  override def insertDoc[T <: Document[_]](document: T): CompletionStage[T] = delegate.insertDoc(document).asJava

  override def insert(
      document: JsonDocument,
      writeSettings: CouchbaseWriteSettings): CompletionStage[JsonDocument] =
    delegate.insert(document, writeSettings).asJava

  override def insertDoc[T <: Document[_]](
      document: T,
      writeSettings: CouchbaseWriteSettings): CompletionStage[T] = delegate.insertDoc(document, writeSettings).asJava

  override def get(id: String): CompletionStage[Optional[JsonDocument]] =
    futureOptToJava(delegate.get(id))

  override def get[T <: Document[_]](id: String, clazz: Class[T]): CompletionStage[Optional[T]] =
    futureOptToJava(delegate.get(id, clazz))

  override def get(id: String, timeout: Duration): CompletionStage[Optional[JsonDocument]] =
    futureOptToJava(delegate.get(id, FiniteDuration.apply(timeout.toNanos, duration.NANOSECONDS)))

  def get[T <: Document[_]](id: String, timeout: Duration, documentClass: Class[T]): CompletionStage[Optional[T]] =
    futureOptToJava(delegate.get(id, FiniteDuration.apply(timeout.toNanos, duration.NANOSECONDS), documentClass))

  override def upsert(document: JsonDocument): CompletionStage[JsonDocument] = delegate.upsert(document).asJava

  override def upsertDoc[T <: Document[_]](document: T): CompletionStage[T] = delegate.upsertDoc(document).asJava

  override def upsert(document: JsonDocument, writeSettings: CouchbaseWriteSettings): CompletionStage[JsonDocument] =
    delegate.upsert(document, writeSettings).asJava

  override def upsertDoc[T <: Document[_]](document: T, writeSettings: CouchbaseWriteSettings): CompletionStage[T] =
    delegate.upsertDoc(document, writeSettings).asJava

  override def replace(document: JsonDocument): CompletionStage[JsonDocument] = delegate.replace(document).asJava

  override def replaceDoc[T <: Document[_]](document: T): CompletionStage[T] = delegate.replaceDoc(document).asJava

  override def replace(document: JsonDocument, writeSettings: CouchbaseWriteSettings): CompletionStage[JsonDocument] =
    delegate.replace(document, writeSettings).asJava

  override def replaceDoc[T <: Document[_]](document: T, writeSettings: CouchbaseWriteSettings): CompletionStage[T] =
    delegate.replaceDoc(document, writeSettings).asJava

  override def remove(id: String): CompletionStage[Done] = delegate.remove(id).asJava

  override def remove(id: String, writeSettings: CouchbaseWriteSettings): CompletionStage[Done] =
    delegate.remove(id, writeSettings).asJava

  override def streamedQuery(query: N1qlQuery): Source[JsonObject, pekko.NotUsed] =
    delegate.streamedQuery(query).asJava

  override def streamedQuery(query: Statement): Source[JsonObject, pekko.NotUsed] =
    delegate.streamedQuery(query).asJava

  override def singleResponseQuery(query: Statement): CompletionStage[Optional[JsonObject]] =
    futureOptToJava(delegate.singleResponseQuery(query))

  override def singleResponseQuery(query: N1qlQuery): CompletionStage[Optional[JsonObject]] =
    futureOptToJava(delegate.singleResponseQuery(query))

  override def counter(id: String, delta: Long, initial: Long): CompletionStage[Long] =
    delegate.counter(id, delta, initial).asJava

  override def counter(
      id: String,
      delta: Long,
      initial: Long,
      writeSettings: CouchbaseWriteSettings): CompletionStage[Long] =
    delegate.counter(id, delta, initial, writeSettings).asJava

  override def close(): CompletionStage[Done] = delegate.close().asJava

  override def createIndex(indexName: String, ignoreIfExist: Boolean, fields: AnyRef*): CompletionStage[Boolean] =
    delegate.createIndex(indexName, ignoreIfExist, fields).asJava

  private def futureOptToJava[T](future: Future[Option[T]]): CompletionStage[Optional[T]] =
    future.map(_.toJava)(ExecutionContexts.parasitic).asJava

  def listIndexes(): Source[IndexInfo, NotUsed] =
    delegate.listIndexes().asJava
}
