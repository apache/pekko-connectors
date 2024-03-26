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
import pekko.stream.Attributes
import pekko.stream.Attributes.Attribute

/**
 * Pekko Stream attributes that are used when materializing GCStorage stream blueprints.
 * @deprecated Use [[pekko.stream.connectors.google.GoogleAttributes]]
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleAttributes", "Alpakka 3.0.0")
@Deprecated
object GCStorageAttributes {

  /**
   * Settings to use for the GCStorage stream
   */
  def settings(settings: GCStorageSettings): Attributes = Attributes(GCStorageSettingsValue(settings))

  /**
   * Config path which will be used to resolve required GCStorage settings
   */
  def settingsPath(path: String): Attributes = Attributes(GCStorageSettingsPath(path))
}

/**
 * @deprecated Use [[pekko.stream.connectors.google.GoogleAttributes]]
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleAttributes", "Alpakka 3.0.0")
@Deprecated
final class GCStorageSettingsPath private (val path: String) extends Attribute

/**
 * @deprecated Use [[pekko.stream.connectors.google.GoogleAttributes]]
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleAttributes", "Alpakka 3.0.0")
@Deprecated
object GCStorageSettingsPath {
  val Default: GCStorageSettingsPath = GCStorageSettingsPath(GCStorageSettings.ConfigPath)

  def apply(path: String): GCStorageSettingsPath = new GCStorageSettingsPath(path)
}

/**
 * @deprecated Use [[pekko.stream.connectors.google.GoogleAttributes]]
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleAttributes", "Alpakka 3.0.0")
@Deprecated
final class GCStorageSettingsValue private (val settings: GCStorageSettings) extends Attribute

/**
 * @deprecated Use [[pekko.stream.connectors.google.GoogleAttributes]]
 */
@deprecated("Use org.apache.pekko.stream.connectors.google.GoogleAttributes", "Alpakka 3.0.0")
@Deprecated
object GCStorageSettingsValue {
  def apply(settings: GCStorageSettings): GCStorageSettingsValue = new GCStorageSettingsValue(settings)
}
