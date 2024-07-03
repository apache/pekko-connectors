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

package org.apache.pekko.stream.connectors.jakartams.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.KillSwitch
import pekko.stream.connectors.jakartams.javadsl
import pekko.stream.connectors.jakartams.scaladsl.JmsConnectorState._
import pekko.stream.scaladsl.Source

trait JmsProducerStatus {

  /**
   * source that provides connector status change information.
   * Only the most recent connector state is buffered if the source is not consumed.
   */
  def connectorState: Source[JmsConnectorState, NotUsed]

}

/**
 * Handle to shut down consumers and to inspect the connectivity to the JMS broker.
 */
trait JmsConsumerControl extends KillSwitch {

  /**
   * source that provides connector status change information.
   * Only the most recent connector state is buffered if the source is not consumed.
   */
  def connectorState: Source[JmsConnectorState, NotUsed]

}

sealed trait JmsConnectorState {
  final def asJava: javadsl.JmsConnectorState = this match {
    case Disconnected  => javadsl.JmsConnectorState.Disconnected
    case Connecting(_) => javadsl.JmsConnectorState.Connecting
    case Connected     => javadsl.JmsConnectorState.Connected
    case Completing    => javadsl.JmsConnectorState.Completing
    case Completed     => javadsl.JmsConnectorState.Completed
    case Failing(_)    => javadsl.JmsConnectorState.Failing
    case Failed(_)     => javadsl.JmsConnectorState.Failed
  }
}

object JmsConnectorState {
  case object Disconnected extends JmsConnectorState
  case class Connecting(attempt: Int) extends JmsConnectorState
  case object Connected extends JmsConnectorState
  case object Completing extends JmsConnectorState
  case object Completed extends JmsConnectorState
  case class Failing(exception: Throwable) extends JmsConnectorState
  case class Failed(exception: Throwable) extends JmsConnectorState
}
