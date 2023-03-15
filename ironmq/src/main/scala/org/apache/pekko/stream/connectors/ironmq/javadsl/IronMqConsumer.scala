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

package org.apache.pekko.stream.connectors.ironmq.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.ironmq._
import pekko.stream.connectors.ironmq.scaladsl.{ IronMqConsumer => ScalaIronMqConsumer }
import pekko.stream.javadsl._

object IronMqConsumer {

  def atMostOnceSource(queueName: String, settings: IronMqSettings): Source[Message, NotUsed] =
    ScalaIronMqConsumer.atMostOnceSource(queueName, settings).asJava

  def atLeastOnceSource(queueName: String, settings: IronMqSettings): Source[CommittableMessage, NotUsed] =
    ScalaIronMqConsumer.atLeastOnceSource(queueName, settings).map(_.asJava).asJava

}
