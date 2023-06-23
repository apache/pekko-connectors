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

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.couchbase.{ CouchbaseSessionSettings, CouchbaseWriteSettings }
import pekko.stream.scaladsl.{ Keep, Sink }
import com.couchbase.client.java.document.{ Document, JsonDocument }

import scala.concurrent.Future

/**
 * Scala API: Factory methods for Couchbase sinks.
 */
object CouchbaseSink {

  /**
   * Create a sink to update or insert a Couchbase [[com.couchbase.client.java.document.JsonDocument JsonDocument]].
   */
  def upsert(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Sink[JsonDocument, Future[Done]] =
    CouchbaseFlow.upsert(sessionSettings, writeSettings, bucketName).toMat(Sink.ignore)(Keep.right)

  /**
   * Create a sink to update or insert a Couchbase document of the given class.
   */
  def upsertDoc[T <: Document[_]](sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Sink[T, Future[Done]] =
    CouchbaseFlow
      .upsertDoc(sessionSettings, writeSettings, bucketName)
      .toMat(Sink.ignore)(Keep.right)

  /**
   * Create a sink to delete documents from Couchbase by `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSettings,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Sink[String, Future[Done]] =
    CouchbaseFlow.delete(sessionSettings, writeSettings, bucketName).toMat(Sink.ignore)(Keep.right)

}
