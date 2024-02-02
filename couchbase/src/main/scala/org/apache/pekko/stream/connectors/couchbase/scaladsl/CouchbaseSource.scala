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

import com.couchbase.client.java.query.QueryResult
import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.couchbase.{ CouchbaseSessionRegistry, CouchbaseSessionSettings }
import pekko.stream.scaladsl.Source

/**
 * Scala API: Factory methods for Couchbase sources.
 */
object CouchbaseSource {

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.json.JsonValue]]s.
   */
  def fromStatement(sessionSettings: CouchbaseSessionSettings,
      statement: String,
      bucketName: String): Source[QueryResult, NotUsed] =
    Source
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Source
          .future(session.map(_.streamedQuery(statement))(materializer.system.dispatcher))
          .flatMapConcat(identity)
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.json.JsonValue]]s.
   */
  def fromN1qlQuery(sessionSettings: CouchbaseSessionSettings,
      query: String,
      bucketName: String): Source[QueryResult, NotUsed] =
    Source
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Source
          .future(session.map(_.streamedQuery(query))(materializer.system.dispatcher))
          .flatMapConcat(identity)
      }
      .mapMaterializedValue(_ => NotUsed)

}
