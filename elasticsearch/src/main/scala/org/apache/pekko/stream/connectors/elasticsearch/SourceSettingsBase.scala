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

package org.apache.pekko.stream.connectors.elasticsearch

import org.apache.pekko
import pekko.util.JavaDurationConverters._
import java.util.concurrent.TimeUnit

import pekko.stream.connectors.elasticsearch.ElasticsearchConnectionSettings

import scala.concurrent.duration.FiniteDuration

/**
 * Configure Elastiscsearch/OpenSearch sources.
 */
abstract class SourceSettingsBase[Version <: ApiVersionBase, S <: SourceSettingsBase[Version, S]] private[connectors] (
    val connection: ElasticsearchConnectionSettings,
    val bufferSize: Int,
    val includeDocumentVersion: Boolean,
    val scrollDuration: FiniteDuration,
    val apiVersion: Version) { this: S =>
  def withConnection(value: ElasticsearchConnectionSettings): S = copy(connection = value)

  def withBufferSize(value: Int): S = copy(bufferSize = value)

  def withScrollDuration(value: FiniteDuration): S = copy(scrollDuration = value)

  def withScrollDuration(value: java.time.Duration): S = copy(scrollDuration = value.asScala)

  /**
   * If includeDocumentVersion is true, '_version' is returned with the search-results
   *  * https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-request-version.html
   *  * https://www.elastic.co/guide/en/elasticsearch/guide/current/optimistic-concurrency-control.html
   */
  def withIncludeDocumentVersion(value: Boolean): S =
    if (includeDocumentVersion == value) this else copy(includeDocumentVersion = value)

  def withApiVersion(value: Version): S =
    if (apiVersion == value) this else copy(apiVersion = value)

  def scroll: String = {
    val scrollString = scrollDuration.unit match {
      case TimeUnit.DAYS         => "d"
      case TimeUnit.HOURS        => "h"
      case TimeUnit.MINUTES      => "m"
      case TimeUnit.SECONDS      => "s"
      case TimeUnit.MILLISECONDS => "ms"
      case TimeUnit.MICROSECONDS => "micros"
      case TimeUnit.NANOSECONDS  => "nanos"
    }

    s"${scrollDuration.length}$scrollString"
  }

  protected def copy(connection: ElasticsearchConnectionSettings = connection,
      bufferSize: Int = bufferSize,
      includeDocumentVersion: Boolean = includeDocumentVersion,
      scrollDuration: FiniteDuration = scrollDuration,
      apiVersion: Version = apiVersion): S;

}
