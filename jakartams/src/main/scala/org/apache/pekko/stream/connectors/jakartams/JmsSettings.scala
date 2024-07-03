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

import jakarta.jms
import org.apache.pekko
import pekko.annotation.DoNotInherit

/**
 * Shared settings for all JMS stages.
 * Used for internal standardization, and not meant to be used by user code.
 */
@DoNotInherit
trait JmsSettings {
  def connectionFactory: jms.ConnectionFactory
  def connectionRetrySettings: ConnectionRetrySettings
  def destination: Option[Destination]
  def credentials: Option[Credentials]
  def sessionCount: Int
  def connectionStatusSubscriptionTimeout: scala.concurrent.duration.FiniteDuration
}
