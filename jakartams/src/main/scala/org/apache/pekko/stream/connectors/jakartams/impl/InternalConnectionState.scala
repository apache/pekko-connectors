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

package org.apache.pekko.stream.connectors.jakartams.impl

import jakarta.jms
import org.apache.pekko
import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi

import scala.concurrent.Future
import scala.util.Try

/**
 * Internal API.
 */
@InternalApi
private[jakartams] trait InternalConnectionState

/**
 * Internal API.
 */
@InternalApi
private[jakartams] object InternalConnectionState {
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
