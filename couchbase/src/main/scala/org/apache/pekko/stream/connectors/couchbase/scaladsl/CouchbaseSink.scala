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

import com.couchbase.client.java.json.JsonObject
import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.couchbase.{ CouchbaseSessionSetting, CouchbaseWriteSettings }
import pekko.stream.scaladsl.{ Keep, Sink }

import scala.concurrent.Future

/**
 * Scala API: Factory methods for Couchbase sinks.
 */
object CouchbaseSink {

  /**
   * Create a sink to update or insert a Couchbase [[com.couchbase.client.java.json.JsonValue]].
   */
  def upsertJson(sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonObject => String): Sink[JsonObject, Future[Done]] =
    CouchbaseFlow
      .upsertJson(sessionSettings, writeSettings, bucketName, getId)
      .toMat(Sink.ignore)(Keep.right)

  /**
   * Create a sink to update or insert a Couchbase document of the given class.
   */
  def upsert[T](sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Sink[T, Future[Done]] =
    CouchbaseFlow
      .upsert(sessionSettings, writeSettings, bucketName, getId)
      .toMat(Sink.ignore)(Keep.right)

  /**
   * Create a sink to delete documents from Couchbase by `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Sink[String, Future[Done]] =
    CouchbaseFlow.delete(sessionSettings, writeSettings, bucketName).toMat(Sink.ignore)(Keep.right)

}
