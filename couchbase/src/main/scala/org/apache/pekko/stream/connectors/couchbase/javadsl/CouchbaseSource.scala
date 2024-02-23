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

import com.couchbase.client.java.analytics.AnalyticsResult
import com.couchbase.client.java.query.QueryResult
import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.couchbase.{scaladsl, CouchbaseSessionSettings}
import pekko.stream.javadsl.Source

/**
 * Java API: Factory methods for Couchbase sources.
 */
object CouchbaseSource {

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.analytics.AnalyticsResult]].
   */
  def fromQuery(sessionSettings: CouchbaseSessionSettings,
                statement: String): Source[QueryResult, NotUsed] =
    scaladsl.CouchbaseSource.fromQuery(sessionSettings, statement).asJava

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.analytics.AnalyticsResult]].
   */
  def fromAnalyticsQuery(sessionSettings: CouchbaseSessionSettings,
                         query: String): Source[AnalyticsResult, NotUsed] =
    scaladsl.CouchbaseSource.fromAnalyticsQuery(sessionSettings, query).asJava

}
