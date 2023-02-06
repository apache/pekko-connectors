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

package org.apache.pekko.stream.connectors.geode.impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.geode.GeodeSettings
import org.apache.pekko.stream.connectors.geode.impl.pdx.DelegatingPdxSerializer
import org.apache.geode.cache.client.{ ClientCache, ClientCacheFactory }
import org.apache.geode.pdx.PdxSerializer

/**
 * Base of all geode client.
 */
@InternalApi
private[geode] abstract class GeodeCache(geodeSettings: GeodeSettings) {

  private lazy val serializer = new DelegatingPdxSerializer(geodeSettings.pdxCompat)

  protected def registerPDXSerializer[V](pdxSerializer: PdxSerializer, clazz: Class[V]): Unit =
    serializer.register(pdxSerializer, clazz)

  /**
   * This method will overloaded to provide server event subscription.
   *
   * @return
   */
  protected def configure(factory: ClientCacheFactory): ClientCacheFactory

  /**
   * Return ClientCacheFactory:
   * <ul>
   * <li>with PDX support</li>
   * <li>configured by sub classes</li>
   * <li>customized by client application</li>
   * </ul>
   */
  final protected def newCacheFactory(): ClientCacheFactory = {
    val factory = configure(new ClientCacheFactory().setPdxSerializer(serializer))
    geodeSettings.configure.map(_(factory)).getOrElse(factory)
  }

  lazy val cache: ClientCache = newCacheFactory().create()

  def close(keepAlive: Boolean = false): Unit = cache.close(keepAlive)

}
