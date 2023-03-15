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

package org.apache.pekko.stream.connectors.influxdb.scaladsl

import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.{ Done, NotUsed }
import pekko.stream.connectors.influxdb.InfluxDbWriteMessage
import pekko.stream.scaladsl.{ Keep, Sink }
import org.influxdb.InfluxDB
import org.influxdb.dto.Point

import scala.concurrent.Future
import scala.collection.immutable

/**
 * API may change.
 */
@ApiMayChange
object InfluxDbSink {

  def create()(implicit influxDB: InfluxDB): Sink[immutable.Seq[InfluxDbWriteMessage[Point, NotUsed]], Future[Done]] =
    InfluxDbFlow.create().toMat(Sink.ignore)(Keep.right)

  def typed[T](
      clazz: Class[T])(
      implicit influxDB: InfluxDB): Sink[immutable.Seq[InfluxDbWriteMessage[T, NotUsed]], Future[Done]] =
    InfluxDbFlow
      .typed(clazz)
      .toMat(Sink.ignore)(Keep.right)

}
