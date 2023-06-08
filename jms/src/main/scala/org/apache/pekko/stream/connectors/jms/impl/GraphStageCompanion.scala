/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.stream.connectors.jms.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.Materializer
import pekko.stream.connectors.jms.Destination

import scala.concurrent.duration.FiniteDuration

/**
 * Exposes some protected methods from [[pekko.stream.stage.GraphStage]]
 * that are not accessible when using Scala3 compiler.
 */
@InternalApi
private[impl] trait GraphStageCompanion {
  def graphStageMaterializer: Materializer

  def graphStageDestination: Destination

  def scheduleOnceOnGraphStage(timerKey: Any, delay: FiniteDuration): Unit

  def isTimerActiveOnGraphStage(timerKey: Any): Boolean

  def cancelTimerOnGraphStage(timerKey: Any): Unit
}
