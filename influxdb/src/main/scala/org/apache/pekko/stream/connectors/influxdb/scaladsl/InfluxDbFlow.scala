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

package org.apache.pekko.stream.connectors.influxdb.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.influxdb.{ impl, InfluxDbWriteMessage, InfluxDbWriteResult }
import pekko.stream.scaladsl.Flow
import org.influxdb.InfluxDB
import org.influxdb.dto.Point

/**
 * Scala API to create InfluxDB flows.
 *
 * API may change.
 */
@ApiMayChange
object InfluxDbFlow {

  def create()(
      implicit influxDB: InfluxDB): Flow[Seq[InfluxDbWriteMessage[Point, NotUsed]],
    Seq[InfluxDbWriteResult[Point, NotUsed]], NotUsed] =
    Flow.fromGraph(new impl.InfluxDbFlowStage[NotUsed](influxDB))

  def typed[T](clazz: Class[T])(
      implicit influxDB: InfluxDB)
      : Flow[Seq[InfluxDbWriteMessage[T, NotUsed]], Seq[InfluxDbWriteResult[T, NotUsed]], NotUsed] =
    Flow.fromGraph(new impl.InfluxDbMapperFlowStage[T, NotUsed](clazz, influxDB))

  def createWithPassThrough[C](
      implicit influxDB: InfluxDB)
      : Flow[Seq[InfluxDbWriteMessage[Point, C]], Seq[InfluxDbWriteResult[Point, C]], NotUsed] =
    Flow.fromGraph(new impl.InfluxDbFlowStage[C](influxDB))

  def typedWithPassThrough[T, C](clazz: Class[T])(
      implicit influxDB: InfluxDB)
      : Flow[Seq[InfluxDbWriteMessage[T, C]], Seq[InfluxDbWriteResult[T, C]], NotUsed] =
    Flow.fromGraph(new impl.InfluxDbMapperFlowStage[T, C](clazz, influxDB))

}
