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

package org.apache.pekko.stream.connectors.amqp

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.dispatch.ExecutionContexts
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

abstract class AmqpSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures with LogCapturing {

  implicit val system = ActorSystem(this.getClass.getSimpleName)
  implicit val executionContext = ExecutionContexts.parasitic

  override protected def afterAll(): Unit =
    system.terminate()
}
