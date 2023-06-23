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

package org.apache.pekko.stream.connectors.jms.impl

import org.apache.pekko
import pekko.Done
import pekko.annotation.InternalApi
import javax.jms

import scala.concurrent.Future
import scala.util.Try

/**
 * Internal API.
 */
@InternalApi
private[jms] trait InternalConnectionState

/**
 * Internal API.
 */
@InternalApi
private[jms] object InternalConnectionState {
  case object JmsConnectorDisconnected extends InternalConnectionState
  case class JmsConnectorInitializing(connection: Future[jms.Connection],
      attempt: Int,
      backoffMaxed: Boolean,
      sessions: Int)
      extends InternalConnectionState
  case class JmsConnectorConnected(connection: jms.Connection) extends InternalConnectionState
  case class JmsConnectorStopping(completion: Try[Done]) extends InternalConnectionState
  case class JmsConnectorStopped(completion: Try[Done]) extends InternalConnectionState
}
