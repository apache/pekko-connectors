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

package docs.scaladsl

import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.MqttConnectionSettings
import akka.stream.alpakka.testkit.scaladsl.LogCapturing
import akka.testkit.TestKit
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

abstract class MqttSpecBase(name: String)
    extends TestKit(ActorSystem(name))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with LogCapturing {
  val connectionSettings = MqttConnectionSettings(
    "tcp://localhost:1883",
    "test-client",
    new MemoryPersistence)

  val timeout = 5.seconds

  override def afterAll() = TestKit.shutdownActorSystem(system)

}
