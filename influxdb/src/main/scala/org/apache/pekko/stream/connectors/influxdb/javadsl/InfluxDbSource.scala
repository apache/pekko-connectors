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

package org.apache.pekko.stream.connectors.influxdb.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.influxdb.InfluxDbReadSettings
import pekko.stream.javadsl.Source
import org.influxdb.InfluxDB
import org.influxdb.dto.{ Query, QueryResult }
import pekko.stream.connectors.influxdb.impl.{ InfluxDbRawSourceStage, InfluxDbSourceStage }

/**
 * Java API to create InfluxDB sources.
 *
 * API may change.
 */
@ApiMayChange
object InfluxDbSource {

  /**
   * Java API: creates an [[InfluxDbRawSourceStage]] from a given statement.
   */
  def create(influxDB: InfluxDB, query: Query): Source[QueryResult, NotUsed] =
    Source.fromGraph(new InfluxDbRawSourceStage(query, influxDB))

  /**
   * Java API: creates an  [[InfluxDbSourceStage]] of elements of `T` from `query`.
   */
  def typed[T](clazz: Class[T], settings: InfluxDbReadSettings, influxDB: InfluxDB, query: Query): Source[T, NotUsed] =
    Source.fromGraph(
      new InfluxDbSourceStage[T](
        clazz,
        settings,
        influxDB,
        query))

}
