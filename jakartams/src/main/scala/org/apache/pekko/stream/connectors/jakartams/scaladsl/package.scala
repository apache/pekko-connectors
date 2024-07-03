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

package org.apache.pekko.stream.connectors.jakartams

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.jakartams.impl.InternalConnectionState
import pekko.stream.scaladsl.Source
import pekko.{ Done, NotUsed }

import scala.util.{ Failure, Success }

package object scaladsl {
  @InternalApi private[scaladsl] def transformConnectorState(source: Source[InternalConnectionState, NotUsed]) = {
    import InternalConnectionState._
    source.map {
      case JmsConnectorDisconnected            => JmsConnectorState.Disconnected
      case _: JmsConnectorConnected            => JmsConnectorState.Connected
      case i: JmsConnectorInitializing         => JmsConnectorState.Connecting(i.attempt + 1)
      case JmsConnectorStopping(Success(Done)) => JmsConnectorState.Completing
      case JmsConnectorStopping(Failure(t))    => JmsConnectorState.Failing(t)
      case JmsConnectorStopped(Success(Done))  => JmsConnectorState.Completed
      case JmsConnectorStopped(Failure(t))     => JmsConnectorState.Failed(t)
      case other                               => throw new MatchError(other)
    }
  }
}
