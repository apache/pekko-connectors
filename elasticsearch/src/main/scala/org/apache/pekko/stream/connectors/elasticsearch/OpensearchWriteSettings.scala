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

/**
 * Configure Opensearch sinks and flows.
 */
final class OpensearchWriteSettings private (connection: ElasticsearchConnectionSettings,
    bufferSize: Int,
    retryLogic: RetryLogic,
    versionType: Option[String],
    apiVersion: OpensearchApiVersion,
    allowExplicitIndex: Boolean)
    extends WriteSettingsBase[OpensearchApiVersion, OpensearchWriteSettings](connection,
      bufferSize,
      retryLogic,
      versionType,
      apiVersion,
      allowExplicitIndex) {

  protected override def copy(connection: ElasticsearchConnectionSettings,
      bufferSize: Int,
      retryLogic: RetryLogic,
      versionType: Option[String],
      apiVersion: OpensearchApiVersion,
      allowExplicitIndex: Boolean): OpensearchWriteSettings =
    new OpensearchWriteSettings(connection, bufferSize, retryLogic, versionType, apiVersion, allowExplicitIndex)

  override def toString: String =
    "OpensearchWriteSettings(" +
    s"connection=$connection," +
    s"bufferSize=$bufferSize," +
    s"retryLogic=$retryLogic," +
    s"versionType=$versionType," +
    s"apiVersion=$apiVersion," +
    s"allowExplicitIndex=$allowExplicitIndex)"

}

object OpensearchWriteSettings {

  /** Scala API */
  def apply(connection: ElasticsearchConnectionSettings): OpensearchWriteSettings =
    new OpensearchWriteSettings(connection, 10, RetryNever, None, OpensearchApiVersion.V1, allowExplicitIndex = true)

  /** Java API */
  def create(connection: ElasticsearchConnectionSettings): OpensearchWriteSettings =
    new OpensearchWriteSettings(connection, 10, RetryNever, None, OpensearchApiVersion.V1, allowExplicitIndex = true)
}
