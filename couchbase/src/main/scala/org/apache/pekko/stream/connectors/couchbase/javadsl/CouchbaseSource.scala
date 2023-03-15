/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.couchbase.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.couchbase.CouchbaseSessionSettings
import pekko.stream.connectors.couchbase.scaladsl
import pekko.stream.javadsl.Source
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.query.{ N1qlQuery, Statement }

/**
 * Java API: Factory methods for Couchbase sources.
 */
object CouchbaseSource {

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.document.JsonDocument JsonDocument]]s.
   */
  def fromStatement(sessionSettings: CouchbaseSessionSettings,
      statement: Statement,
      bucketName: String): Source[JsonObject, NotUsed] =
    scaladsl.CouchbaseSource.fromStatement(sessionSettings, statement, bucketName).asJava

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.document.JsonDocument JsonDocument]]s.
   */
  def fromN1qlQuery(sessionSettings: CouchbaseSessionSettings,
      query: N1qlQuery,
      bucketName: String): Source[JsonObject, NotUsed] =
    scaladsl.CouchbaseSource.fromN1qlQuery(sessionSettings, query, bucketName).asJava

}
