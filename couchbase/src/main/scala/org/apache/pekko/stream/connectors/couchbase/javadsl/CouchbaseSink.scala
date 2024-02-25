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
import pekko.stream.connectors.couchbase.{ CouchbaseSessionSettings, CouchbaseWriteSettings }
import pekko.stream.javadsl.{ Keep, Sink }
import pekko.{ Done, NotUsed }

import com.couchbase.client.java.json.{ JsonObject, JsonValue }

import java.util.concurrent.CompletionStage

/**
 * Java API: Factory methods for Couchbase sinks.
 */
object CouchbaseSink {

  /**
   * Create a sink to update or insert a Couchbase [[com.couchbase.client.java.document.JsonDocument JsonDocument]].
   */
  def upsertJson(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonObject => String): Sink[JsonObject, CompletionStage[Done]] =
    CouchbaseFlow
      .upsertJson(sessionSettings, writeSettings, bucketName, getId)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a sink to update or insert a Couchbase document of the given class.
   */
  def upsert[T](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Sink[T, CompletionStage[Done]] =
    CouchbaseFlow
      .upsert[T](sessionSettings, writeSettings, bucketName, getId)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a sink to replace a Couchbase [[com.couchbase.client.java.document.JsonDocument JsonDocument]].
   */
  def replaceJson(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonValue => String): Sink[JsonValue, CompletionStage[Done]] =
    CouchbaseFlow
      .replaceJson(sessionSettings, writeSettings, bucketName, getId)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a sink to replace a Couchbase document of the given class.
   */
  def replace[T](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String, getId: T => String): Sink[T, CompletionStage[Done]] =
    CouchbaseFlow
      .replace[T](sessionSettings, writeSettings, bucketName, getId)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a sink to delete documents from Couchbase by `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Sink[String, CompletionStage[Done]] =
    CouchbaseFlow
      .delete(sessionSettings, writeSettings, bucketName)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

}
