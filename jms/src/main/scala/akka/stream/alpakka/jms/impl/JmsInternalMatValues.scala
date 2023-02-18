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

package akka.stream.alpakka.jms.impl

import akka.NotUsed
import akka.annotation.InternalApi
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source

/**
 * Internal API.
 */
@InternalApi private[jms] trait JmsProducerMatValue {
  def connected: Source[InternalConnectionState, NotUsed]
}

/**
 * Internal API.
 */
@InternalApi private[jms] trait JmsConsumerMatValue extends KillSwitch with JmsProducerMatValue
