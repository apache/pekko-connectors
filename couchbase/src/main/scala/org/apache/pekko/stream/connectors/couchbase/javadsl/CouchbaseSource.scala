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

import com.couchbase.client.java.json.JsonObject
import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.couchbase.{ scaladsl, CouchbaseSessionSetting }
import pekko.stream.javadsl.Source

import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * Java API: Factory methods for Couchbase sources.
 */
object CouchbaseSource {

  /**
   * Create a source query Couchbase by statement, emitted as [[com.couchbase.client.java.analytics.AnalyticsResult]].
   */
  def fromQueryJson(
      sessionSetting: CouchbaseSessionSetting, statement: String): Source[java.util.List[JsonObject], NotUsed] =
    scaladsl.CouchbaseSource.fromQueryJson(sessionSetting, statement).map(_.asJava).asJava

  def fromQuery[T](sessionSettings: CouchbaseSessionSetting, statement: String, target: Class[T])
      : Source[java.util.List[T], NotUsed] =
    scaladsl.CouchbaseSource.fromQuery(sessionSettings, statement, target).map(_.asJava).asJava

}
