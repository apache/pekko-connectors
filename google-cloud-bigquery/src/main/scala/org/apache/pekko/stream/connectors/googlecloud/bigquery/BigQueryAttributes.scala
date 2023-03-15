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
import pekko.stream.Attributes.Attribute
import pekko.stream.{ Attributes, Materializer }

/**
 * Akka Stream [[Attributes]] that are used when materializing BigQuery stream blueprints.
 */
object BigQueryAttributes {

  /**
   * [[BigQuerySettings]] to use for the BigQuery stream
   */
  def settings(settings: BigQuerySettings): Attributes = Attributes(BigQuerySettingsValue(settings))

  /**
   * Config path which will be used to resolve [[BigQuerySettings]]
   */
  def settingsPath(path: String): Attributes = Attributes(BigQuerySettingsPath(path))

  /**
   * Resolves the most specific [[BigQuerySettings]] for some [[Attributes]]
   */
  def resolveSettings(mat: Materializer, attr: Attributes): BigQuerySettings =
    attr.attributeList.collectFirst {
      case BigQuerySettingsValue(settings) => settings
      case BigQuerySettingsPath(path)      => BigQueryExt(mat.system).settings(path)
    }.getOrElse {
      BigQueryExt(mat.system).settings
    }

  private final case class BigQuerySettingsValue(settings: BigQuerySettings) extends Attribute
  private final case class BigQuerySettingsPath(path: String) extends Attribute
}
