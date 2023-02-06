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

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
 * Configure Opensearch sources.
 */
final class OpensearchSourceSettings private (connection: ElasticsearchConnectionSettings,
    bufferSize: Int,
    includeDocumentVersion: Boolean,
    scrollDuration: FiniteDuration,
    apiVersion: OpensearchApiVersion)
    extends SourceSettingsBase[OpensearchApiVersion, OpensearchSourceSettings](connection,
      bufferSize,
      includeDocumentVersion,
      scrollDuration,
      apiVersion) {
  protected override def copy(connection: ElasticsearchConnectionSettings,
      bufferSize: Int,
      includeDocumentVersion: Boolean,
      scrollDuration: FiniteDuration,
      apiVersion: OpensearchApiVersion): OpensearchSourceSettings =
    new OpensearchSourceSettings(connection = connection,
      bufferSize = bufferSize,
      includeDocumentVersion = includeDocumentVersion,
      scrollDuration = scrollDuration,
      apiVersion = apiVersion)

  override def toString =
    s"""OpensearchSourceSettings(connection=$connection,bufferSize=$bufferSize,includeDocumentVersion=$includeDocumentVersion,scrollDuration=$scrollDuration,apiVersion=$apiVersion)"""

}

object OpensearchSourceSettings {

  /** Scala API */
  def apply(connection: ElasticsearchConnectionSettings): OpensearchSourceSettings =
    new OpensearchSourceSettings(connection,
      10,
      includeDocumentVersion = false,
      FiniteDuration(5, TimeUnit.MINUTES),
      OpensearchApiVersion.V1)

  /** Java API */
  def create(connection: ElasticsearchConnectionSettings): OpensearchSourceSettings =
    new OpensearchSourceSettings(connection,
      10,
      includeDocumentVersion = false,
      FiniteDuration(5, TimeUnit.MINUTES),
      OpensearchApiVersion.V1)
}
