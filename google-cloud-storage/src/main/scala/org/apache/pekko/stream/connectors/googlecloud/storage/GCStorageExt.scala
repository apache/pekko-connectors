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
 * Manages one [[GCStorageSettings]] per `ActorSystem`.
 * @deprecated Use [[pekko.stream.connectors.google.GoogleSettings]].
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleSettings", "Alpakka 3.0.0")
@Deprecated
final class GCStorageExt private (sys: ExtendedActorSystem) extends Extension {
  val settings: GCStorageSettings = settings(GCStorageSettings.ConfigPath)

  def settings(prefix: String): GCStorageSettings = GCStorageSettings(sys.settings.config.getConfig(prefix))
}

/**
 * @deprecated Use [[pekko.stream.connectors.google.GoogleSettings]]
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleSettings", "Alpakka 3.0.0")
@Deprecated
object GCStorageExt extends ExtensionId[GCStorageExt] with ExtensionIdProvider {
  override def lookup: GCStorageExt.type = GCStorageExt
  override def createExtension(system: ExtendedActorSystem) = new GCStorageExt(system)

  /**
   * Java API.
   * Get the GCS extension with the classic actors API.
   */
  override def get(system: pekko.actor.ActorSystem): GCStorageExt = super.apply(system)

  /**
   * Java API.
   * Get the GCS extension with the new actors API.
   */
  override def get(system: ClassicActorSystemProvider): GCStorageExt = super.apply(system)
}
