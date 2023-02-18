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

package akka.stream.alpakka.googlecloud.storage

import akka.stream.Attributes
import akka.stream.Attributes.Attribute

object GCSAttributes {

  /**
   * Settings to use for the GCS stream
   */
  def settings(settings: GCSSettings): Attributes = Attributes(GCSSettingsValue(settings))

  /**
   * Config path which will be used to resolve required GCStorage settings
   */
  def settingsPath(path: String): Attributes = Attributes(GCSSettingsPath(path))

}

final class GCSSettingsPath private (val path: String) extends Attribute

object GCSSettingsPath {
  val Default = GCSSettingsPath(GCSSettings.ConfigPath)

  def apply(path: String) = new GCSSettingsPath(path)
}

final class GCSSettingsValue private (val settings: GCSSettings) extends Attribute

object GCSSettingsValue {
  def apply(settings: GCSSettings) = new GCSSettingsValue(settings)
}
