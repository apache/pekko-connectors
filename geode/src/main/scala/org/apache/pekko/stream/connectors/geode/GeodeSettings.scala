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

package org.apache.pekko.stream.connectors.geode

import org.apache.geode.cache.client.ClientCacheFactory

/**
 * General settings to connect to Apache Geode.
 */
final class GeodeSettings private (val hostname: String,
    val port: Int = 10334,
    val configure: Option[ClientCacheFactory => ClientCacheFactory] = None,
    val pdxCompat: (Class[_], Class[_]) => Boolean = (c1, c2) =>
      c1.getSimpleName.equals(c2.getSimpleName)) {

  private def copy(hostname: String = hostname,
      port: Int = port,
      configure: Option[ClientCacheFactory => ClientCacheFactory] = configure,
      pdxCompat: (Class[_], Class[_]) => Boolean = pdxCompat) =
    new GeodeSettings(hostname, port, configure, pdxCompat)

  /**
   * @param configure function to configure geode client factory
   */
  def withConfiguration(configure: ClientCacheFactory => ClientCacheFactory): GeodeSettings =
    copy(configure = Some(configure))

  /**
   * @param pdxCompat a function that determines if two class are equivalent (java class / scala case class)
   */
  def withPdxCompat(pdxCompat: (Class[_], Class[_]) => Boolean): GeodeSettings = copy(pdxCompat = pdxCompat)

  override def toString: String =
    "GeodeSettings(" +
    s"hostname=$hostname," +
    s"port=$port," +
    s"configuration=${configure.isDefined}" +
    ")"

}

object GeodeSettings {

  def apply(hostname: String, port: Int = 10334): GeodeSettings = new GeodeSettings(hostname, port)

  def create(hostname: String): GeodeSettings = new GeodeSettings(hostname)

  def create(hostname: String, port: Int): GeodeSettings = new GeodeSettings(hostname, port)

}

final class RegionSettings[K, V] private (val name: String, val keyExtractor: V => K) {
  override def toString: String =
    "RegionSettings(" +
    s"name=$name" +
    ")"
}

object RegionSettings {

  def apply[K, V](name: String, keyExtractor: V => K): RegionSettings[K, V] = new RegionSettings(name, keyExtractor)

  def create[K, V](name: String, keyExtractor: V => K): RegionSettings[K, V] =
    new RegionSettings(name, keyExtractor)

}
