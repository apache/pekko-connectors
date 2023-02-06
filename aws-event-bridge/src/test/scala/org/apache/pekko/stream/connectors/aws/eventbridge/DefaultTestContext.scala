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

package org.apache.pekko.stream.connectors.aws.eventbridge

import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito.reset
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Suite }
import org.scalatestplus.mockito.MockitoSugar
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient

import scala.concurrent.Await
import scala.concurrent.duration._

trait DefaultTestContext extends BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar { this: Suite =>

  implicit protected val system: ActorSystem = ActorSystem()
  implicit protected val eventBridgeClient: EventBridgeAsyncClient = mock[EventBridgeAsyncClient]

  override protected def beforeEach(): Unit =
    reset(eventBridgeClient)

  override protected def afterAll(): Unit =
    Await.ready(system.terminate(), 5.seconds)

}
