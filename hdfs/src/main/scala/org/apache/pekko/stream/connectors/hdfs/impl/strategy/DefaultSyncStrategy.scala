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

package org.apache.pekko.stream.connectors.hdfs.impl.strategy

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.hdfs.SyncStrategy

/**
 * Internal API
 */
@InternalApi
private[hdfs] object DefaultSyncStrategy {
  final case class CountSyncStrategy(
      executeCount: Long = 0,
      count: Long) extends SyncStrategy {
    def should(): Boolean = executeCount >= count
    def reset(): SyncStrategy = copy(executeCount = 0)
    def update(offset: Long): SyncStrategy = copy(executeCount = executeCount + 1)
  }

  case object NoSyncStrategy extends SyncStrategy {
    def should(): Boolean = false
    def reset(): SyncStrategy = this
    def update(offset: Long): SyncStrategy = this
  }
}
