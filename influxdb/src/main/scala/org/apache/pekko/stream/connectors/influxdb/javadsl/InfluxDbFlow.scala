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

package org.apache.pekko.stream.connectors.influxdb.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.influxdb.{ InfluxDbWriteMessage, InfluxDbWriteResult }
import org.influxdb.InfluxDB
import pekko.stream.javadsl.Flow
import pekko.stream.connectors.influxdb.scaladsl
import pekko.util.ccompat.JavaConverters._
import org.influxdb.dto.Point

/**
 * API may change.
 */
@ApiMayChange
object InfluxDbFlow {

  def create(
      influxDB: InfluxDB): Flow[java.util.List[InfluxDbWriteMessage[Point, NotUsed]],
    java.util.List[InfluxDbWriteResult[Point, NotUsed]], NotUsed] =
    pekko.stream.scaladsl
      .Flow[java.util.List[InfluxDbWriteMessage[Point, NotUsed]]]
      .map(_.asScala.toList)
      .via(scaladsl.InfluxDbFlow.create()(influxDB))
      .map(_.asJava)
      .asJava

  def typed[T](
      clazz: Class[T],
      influxDB: InfluxDB): Flow[java.util.List[InfluxDbWriteMessage[T, NotUsed]], java.util.List[InfluxDbWriteResult[T,
      NotUsed]], NotUsed] =
    pekko.stream.scaladsl
      .Flow[java.util.List[InfluxDbWriteMessage[T, NotUsed]]]
      .map(_.asScala.toList)
      .via(scaladsl.InfluxDbFlow.typed(clazz)(influxDB))
      .map(_.asJava)
      .asJava

  def createWithPassThrough[C](
      influxDB: InfluxDB)
      : Flow[java.util.List[InfluxDbWriteMessage[Point, C]], java.util.List[InfluxDbWriteResult[Point, C]], NotUsed] =
    pekko.stream.scaladsl
      .Flow[java.util.List[InfluxDbWriteMessage[Point, C]]]
      .map(_.asScala.toList)
      .via(scaladsl.InfluxDbFlow.createWithPassThrough(influxDB))
      .map(_.asJava)
      .asJava

  def typedWithPassThrough[T, C](
      clazz: Class[T],
      influxDB: InfluxDB)
      : Flow[java.util.List[InfluxDbWriteMessage[T, C]], java.util.List[InfluxDbWriteResult[T, C]], NotUsed] =
    pekko.stream.scaladsl
      .Flow[java.util.List[InfluxDbWriteMessage[T, C]]]
      .map(_.asScala.toList)
      .via(scaladsl.InfluxDbFlow.typedWithPassThrough(clazz)(influxDB))
      .map(_.asJava)
      .asJava

}
