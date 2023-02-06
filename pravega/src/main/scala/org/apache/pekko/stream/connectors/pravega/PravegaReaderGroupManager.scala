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

package org.apache.pekko.stream.connectors.pravega

import io.pravega.client.ClientConfig
import io.pravega.client.admin.ReaderGroupManager
import io.pravega.client.stream.ReaderGroupConfig.ReaderGroupConfigBuilder
import io.pravega.client.stream.{ ReaderGroup, ReaderGroupConfig, Stream => PravegaStream }

import scala.annotation.varargs

final class PravegaReaderGroupManager(scope: String,
    clientConfig: ClientConfig,
    readerGroupConfigBuilder: ReaderGroupConfigBuilder)
    extends AutoCloseable {

  def this(scope: String, clientConfig: ClientConfig) =
    this(scope,
      clientConfig,
      ReaderGroupConfig
        .builder())

  private lazy val readerGroupManager = ReaderGroupManager.withScope(scope, clientConfig)

  @varargs def createReaderGroup[A](groupName: String, streamNames: String*): ReaderGroup = {

    streamNames
      .map(PravegaStream.of(scope, _))
      .foreach(readerGroupConfigBuilder.stream)

    readerGroupManager.createReaderGroup(groupName, readerGroupConfigBuilder.build())
    readerGroupManager.getReaderGroup(groupName)
  }

  override def close(): Unit = readerGroupManager.close()
}
