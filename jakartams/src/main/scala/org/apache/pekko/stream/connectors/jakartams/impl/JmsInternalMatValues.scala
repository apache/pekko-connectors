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

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.KillSwitch
import pekko.stream.scaladsl.Source

/**
 * Internal API.
 */
@InternalApi private[jakartams] trait JmsProducerMatValue {
  def connected: Source[InternalConnectionState, NotUsed]
}

/**
 * Internal API.
 */
@InternalApi private[jakartams] trait JmsConsumerMatValue extends KillSwitch with JmsProducerMatValue
