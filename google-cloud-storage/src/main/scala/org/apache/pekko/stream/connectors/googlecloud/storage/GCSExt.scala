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

package org.apache.pekko.stream.connectors.googlecloud.storage

import org.apache.pekko
import pekko.actor.{ ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }

/**
 * Manages one [[GCSSettings]] per `ActorSystem`.
 */
final class GCSExt private (sys: ExtendedActorSystem) extends Extension {
  val settings: GCSSettings = settings(GCSSettings.ConfigPath)

  def settings(prefix: String): GCSSettings = GCSSettings(sys.settings.config.getConfig(prefix))
}

object GCSExt extends ExtensionId[GCSExt] with ExtensionIdProvider {
  override def lookup = GCSExt
  override def createExtension(system: ExtendedActorSystem) = new GCSExt(system)

  /**
   * Java API.
   * Get the GCS extension with the classic actors API.
   */
  override def get(system: pekko.actor.ActorSystem): GCSExt = super.apply(system)

  /**
   * Java API.
   * Get the GCS extension with the new actors API.
   */
  override def get(system: ClassicActorSystemProvider): GCSExt = super.apply(system)
}
