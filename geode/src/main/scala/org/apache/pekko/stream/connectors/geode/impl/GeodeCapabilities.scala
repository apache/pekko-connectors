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

package org.apache.pekko.stream.connectors.geode.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.geode.RegionSettings
import pekko.stream.stage.StageLogging
import org.apache.geode.cache.client.{ ClientCache, ClientRegionShortcut }

import scala.util.control.NonFatal
@InternalApi
private[geode] trait GeodeCapabilities[K, V] { this: StageLogging =>

  def regionSettings: RegionSettings[K, V]

  def clientCache: ClientCache

  private lazy val region =
    clientCache.createClientRegionFactory[K, V](ClientRegionShortcut.CACHING_PROXY).create(regionSettings.name)

  def put(v: V): Unit = region.put(regionSettings.keyExtractor(v), v)

  def close(): Unit =
    try {
      if (clientCache.isClosed)
        return
      region.close()
      log.debug("region closed")
    } catch {
      case NonFatal(ex) => log.error(ex, "Problem occurred during producer region closing")
    }
}
