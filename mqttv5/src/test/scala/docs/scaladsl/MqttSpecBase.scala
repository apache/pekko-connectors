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

package docs.scaladsl

import scala.concurrent.duration._

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.testkit.TestKit
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

abstract class MqttSpecBase(name: String)
    extends TestKit(ActorSystem(s"${name}_v5"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with LogCapturing {
  val connectionSettings: MqttConnectionSettings = MqttConnectionSettings(
    broker = "tcp://localhost:1883",
    clientId = "test-client",
    persistence = new MemoryPersistence
  )

  val timeout: FiniteDuration = 5.seconds

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
