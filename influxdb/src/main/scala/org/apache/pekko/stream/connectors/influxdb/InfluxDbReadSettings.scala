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

package org.apache.pekko.stream.connectors.influxdb

import java.util.concurrent.TimeUnit

import org.apache.pekko.annotation.ApiMayChange

/**
 * API may change.
 */
@ApiMayChange
object InfluxDbReadSettings {
  val Default = new InfluxDbReadSettings(TimeUnit.MILLISECONDS)

  def apply(): InfluxDbReadSettings = Default

}

/**
 * API may change.
 */
@ApiMayChange
final class InfluxDbReadSettings private (val precision: TimeUnit) {

  def withPrecision(precision: TimeUnit): InfluxDbReadSettings = copy(precision = precision)

  private def copy(
      precision: TimeUnit): InfluxDbReadSettings = new InfluxDbReadSettings(
    precision = precision)

  override def toString: String =
    s"""InfluxDbReadSettings(precision=$precision)"""

}
