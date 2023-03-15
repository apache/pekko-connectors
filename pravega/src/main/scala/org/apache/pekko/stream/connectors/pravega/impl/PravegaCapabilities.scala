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

package org.apache.pekko.stream.connectors.pravega.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.stage.StageLogging
import io.pravega.client.ClientConfig
import io.pravega.client.EventStreamClientFactory

import scala.util.{ Failure, Success, Try }

@InternalApi private[pravega] trait PravegaCapabilities {
  this: StageLogging =>

  protected val scope: String
  protected val clientConfig: ClientConfig

  lazy val eventStreamClientFactory = EventStreamClientFactory.withScope(scope, clientConfig)

  def close() = Try(eventStreamClientFactory.close()) match {
    case Failure(exception) =>
      log.error(exception, "Error while closing scope [{}]", scope)
    case Success(value) =>
      log.debug("Closed scope [{}]", scope)
  }

}
