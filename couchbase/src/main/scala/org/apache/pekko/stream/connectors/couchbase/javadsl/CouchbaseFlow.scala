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

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.couchbase._
import pekko.stream.javadsl.Flow

import com.couchbase.client.java.json.{ JsonObject, JsonValue }
import com.couchbase.client.java.kv.{ GetResult, MutationResult }

/**
 * Java API: Factory methods for Couchbase flows.
 */
object CouchbaseFlow {

  /**
   * Create a flow to query Couchbase for by `id` and emit [[com.couchbase.client.java.kv.GetResult]].
   */
  def fromId(sessionSettings: CouchbaseSessionSettings, bucketName: String): Flow[String, GetResult, NotUsed] =
    scaladsl.CouchbaseFlow.fromId(sessionSettings, bucketName).asJava

  /**
   * Create a flow to query Couchbase for by `id` and emit documents of the given class.
   */
  def fromId[T](sessionSettings: CouchbaseSessionSettings,
      bucketName: String,
      target: Class[T]): Flow[String, T, NotUsed] =
    scaladsl.CouchbaseFlow.fromId(sessionSettings, bucketName, target).asJava

  /**
   * Create a flow to update or insert a Couchbase [[com.couchbase.client.java.kv.GetResult]].
   */
  def upsertJson(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonObject => String): Flow[JsonObject, MutationResult, NotUsed] =
    scaladsl.CouchbaseFlow.upsertJson(sessionSettings, writeSettings, bucketName, getId).asJava

  /**
   * Create a flow to update or insert a Couchbase document of the given class.
   */
  def upsert[T](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, MutationResult, NotUsed] =
    scaladsl.CouchbaseFlow.upsert(sessionSettings, writeSettings, bucketName, getId).asJava

  /**
   * Create a flow to update or insert a Couchbase document of the given class and emit a result so that write failures
   * can be handled in-stream.
   */
  def upsertWithResult[T](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, CouchbaseWriteResult[T], NotUsed] =
    scaladsl.CouchbaseFlow.upsertWithResult(sessionSettings, writeSettings, bucketName, getId).asJava

  /**
   * Create a flow to replace a Couchbase [[com.couchbase.client.java.json.JsonValue]].
   */
  def replaceJson(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonValue => String): Flow[JsonValue, MutationResult, NotUsed] =
    scaladsl.CouchbaseFlow.replace(sessionSettings, writeSettings, bucketName, getId).asJava

  /**
   * Create a flow to replace a Couchbase document of the given class.
   */
  def replace[T](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, MutationResult, NotUsed] =
    scaladsl.CouchbaseFlow.replace(sessionSettings, writeSettings, bucketName, getId).asJava

  /**
   * Create a flow to replace a Couchbase document of the given class and emit a result so that write failures
   * can be handled in-stream.
   */
  def replaceWithResult[T](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, CouchbaseWriteResult[T], NotUsed] =
    scaladsl.CouchbaseFlow.replaceWithResult(sessionSettings, writeSettings, bucketName, getId).asJava

  /**
   * Create a flow to delete documents from Couchbase by `id`. Emits the same `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Flow[String, String, NotUsed] =
    scaladsl.CouchbaseFlow.delete(sessionSettings, writeSettings, bucketName).asJava

  /**
   * Create a flow to delete documents from Couchbase by `id` and emit operation outcome containing the same `id`.
   */
  def deleteWithResult(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Flow[String, CouchbaseDeleteResult, NotUsed] =
    scaladsl.CouchbaseFlow.deleteWithResult(sessionSettings, writeSettings, bucketName).asJava

}
