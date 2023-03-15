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

package org.apache.pekko.stream.connectors.googlecloud.bigquery

import org.apache.pekko
import pekko.actor.{
  ActorSystem,
  ClassicActorSystemProvider,
  ExtendedActorSystem,
  Extension,
  ExtensionId,
  ExtensionIdProvider
}
import pekko.annotation.InternalApi

import scala.collection.immutable.ListMap

/**
 * Manages one [[BigQuerySettings]] per `ActorSystem`.
 */
@InternalApi
private[bigquery] final class BigQueryExt private (sys: ExtendedActorSystem) extends Extension {
  private var cachedSettings: Map[String, BigQuerySettings] = ListMap.empty
  val settings: BigQuerySettings = settings(BigQuerySettings.ConfigPath)

  def settings(path: String): BigQuerySettings =
    cachedSettings.getOrElse(path, {
        val settings = BigQuerySettings(sys.settings.config.getConfig(path))
        cachedSettings += path -> settings
        settings
      })
}

@InternalApi
private[bigquery] object BigQueryExt extends ExtensionId[BigQueryExt] with ExtensionIdProvider {

  def apply()(implicit system: ActorSystem): BigQueryExt = super.apply(system)

  override def lookup = BigQueryExt
  override def createExtension(system: ExtendedActorSystem) = new BigQueryExt(system)

  /**
   * Java API.
   * Get the BigQuery extension with the classic actors API.
   */
  override def get(system: pekko.actor.ActorSystem): BigQueryExt = super.apply(system)

  /**
   * Java API.
   * Get the BigQuery extension with the new actors API.
   */
  override def get(system: ClassicActorSystemProvider): BigQueryExt = super.apply(system)
}
