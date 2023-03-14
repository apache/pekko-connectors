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

package org.apache.pekko.stream.connectors.hbase.scaladsl

import org.apache.pekko.stream.connectors.hbase.HTableSettings
import org.apache.pekko.stream.connectors.hbase.impl.{ HBaseFlowStage, HBaseSourceStage }
import org.apache.pekko.stream.scaladsl.{ Flow, Keep, Sink, Source }
import org.apache.pekko.{ Done, NotUsed }
import org.apache.hadoop.hbase.client.{ Result, Scan }

import scala.concurrent.Future

object HTableStage {

  /**
   * Writes incoming element to HBase.
   * HBase mutations for every incoming element are derived from the converter functions defined in the config.
   */
  def sink[A](config: HTableSettings[A]): Sink[A, Future[Done]] =
    Flow[A].via(flow(config)).toMat(Sink.ignore)(Keep.right)

  /**
   * Writes incoming element to HBase.
   * HBase mutations for every incoming element are derived from the converter functions defined in the config.
   */
  def flow[A](settings: HTableSettings[A]): Flow[A, A, NotUsed] =
    Flow.fromGraph(new HBaseFlowStage[A](settings))

  /**
   * Reads an element from HBase.
   */
  def source[A](scan: Scan, settings: HTableSettings[A]): Source[Result, NotUsed] =
    Source.fromGraph(new HBaseSourceStage[A](scan, settings))

}
