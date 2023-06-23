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

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.{ Done, NotUsed }
import pekko.stream.connectors.influxdb.{ InfluxDbWriteMessage, InfluxDbWriteResult }
import pekko.stream.javadsl.{ Keep, Sink }
import org.influxdb.InfluxDB
import org.influxdb.dto.Point

/**
 * Java API.
 *
 * API may change.
 */
@ApiMayChange
object InfluxDbSink {

  def create(
      influxDB: InfluxDB)
      : pekko.stream.javadsl.Sink[java.util.List[InfluxDbWriteMessage[Point, NotUsed]], CompletionStage[Done]] =
    InfluxDbFlow
      .create(influxDB)
      .toMat(Sink.ignore[java.util.List[InfluxDbWriteResult[Point, NotUsed]]](),
        Keep.right[NotUsed, CompletionStage[Done]])

  def typed[T](
      clazz: Class[T],
      influxDB: InfluxDB)
      : pekko.stream.javadsl.Sink[java.util.List[InfluxDbWriteMessage[T, NotUsed]], CompletionStage[Done]] =
    InfluxDbFlow
      .typed(clazz, influxDB)
      .toMat(Sink.ignore[java.util.List[InfluxDbWriteResult[T, NotUsed]]](), Keep.right[NotUsed, CompletionStage[Done]])

}
