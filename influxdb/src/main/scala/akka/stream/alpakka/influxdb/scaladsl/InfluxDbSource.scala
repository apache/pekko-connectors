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

package akka.stream.alpakka.influxdb.scaladsl

import akka.NotUsed
import akka.annotation.ApiMayChange
import akka.stream.alpakka.influxdb.InfluxDbReadSettings
import akka.stream.alpakka.influxdb.impl.{ InfluxDbRawSourceStage, InfluxDbSourceStage }
import akka.stream.scaladsl.Source
import org.influxdb.InfluxDB
import org.influxdb.dto.{ Query, QueryResult }

/**
 * Scala API.
 *
 * API may change.
 */
@ApiMayChange
object InfluxDbSource {

  /**
   * Scala API: creates an [[akka.stream.alpakka.influxdb.impl.InfluxDbRawSourceStage]] from a given statement.
   */
  def apply(influxDB: InfluxDB, query: Query): Source[QueryResult, NotUsed] =
    Source.fromGraph(new InfluxDbRawSourceStage(query, influxDB))

  /**
   * Read elements of `T` from `className` or by `query`.
   */
  def typed[T](clazz: Class[T], settings: InfluxDbReadSettings, influxDB: InfluxDB, query: Query): Source[T, NotUsed] =
    Source.fromGraph(
      new InfluxDbSourceStage[T](
        clazz,
        settings,
        influxDB,
        query))

}
