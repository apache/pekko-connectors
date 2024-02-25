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
import com.couchbase.client.java.query.QueryResult
import org.apache.pekko
import org.apache.pekko.util.FutureConverters.CompletionStageOps
import pekko.NotUsed
import pekko.stream.connectors.couchbase.{ CouchbaseSessionRegistry, CouchbaseSessionSettings }
import pekko.stream.scaladsl.Source

import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * Scala API: Factory methods for Couchbase sources.
 */
object CouchbaseSource {

  private def queryResult(sessionSettings: CouchbaseSessionSettings, statement: String): Source[QueryResult, NotUsed] =
    Source
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        implicit val exec = materializer.system.dispatcher
        Source.future(session.flatMap(_.underlying.query(statement).asScala))
      }.mapMaterializedValue(_ => NotUsed)

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.json.JsonValue]]s.
   */
  def fromQueryJson(sessionSettings: CouchbaseSessionSettings, statement: String): Source[List[JsonObject], NotUsed] =
    queryResult(sessionSettings, statement).map(_.rowsAsObject().asScala.toList)

  def fromQuery[T](
      sessionSettings: CouchbaseSessionSettings, statement: String, target: Class[T]): Source[List[T], NotUsed] =
    queryResult(sessionSettings, statement).map(_.rowsAs(target).asScala.toList)

}
