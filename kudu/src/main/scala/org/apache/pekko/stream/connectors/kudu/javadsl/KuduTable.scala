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

package org.apache.pekko.stream.connectors.kudu.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko.stream.connectors.kudu.KuduTableSettings
import org.apache.pekko.stream.javadsl.{ Flow, Keep, Sink }
import org.apache.pekko.stream.connectors.kudu.scaladsl
import org.apache.pekko.{ Done, NotUsed }

/**
 * Java API
 */
object KuduTable {

  /**
   * Create a Sink writing elements to a Kudu table.
   */
  def sink[A](settings: KuduTableSettings[A]): Sink[A, CompletionStage[Done]] =
    flow(settings).toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a Flow writing elements to a Kudu table.
   */
  def flow[A](settings: KuduTableSettings[A]): Flow[A, A, NotUsed] =
    scaladsl.KuduTable.flow(settings).asJava

}
