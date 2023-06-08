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

import org.apache.pekko
import pekko.actor
import pekko.actor.typed.scaladsl.Behaviors
import pekko.stream.connectors.mqtt.streaming.MqttSessionSettings
import pekko.stream.connectors.mqtt.streaming.scaladsl.{ ActorMqttClientSession, ActorMqttServerSession }
import org.scalatest.wordspec.AnyWordSpec

class MqttTypedActorSystemSpec extends AnyWordSpec {

  implicit val actorSystem: actor.typed.ActorSystem[Nothing] =
    actor.typed.ActorSystem(Behaviors.ignore, "MqttTypedActorSystemSpec")

  "A typed actor system" should {
    "allow client creation" in {
      val settings = MqttSessionSettings()
      val session = ActorMqttClientSession(settings)
      session.shutdown()
    }

    "allow server creation" in {
      val settings = MqttSessionSettings()
      val session = ActorMqttServerSession(settings)
      session.shutdown()
    }
  }

}

class MqttClassicActorSystemSpec extends AnyWordSpec {

  implicit val actorSystem: actor.ActorSystem = actor.ActorSystem("MqttClassicActorSystemSpec")

  "A typed actor system" should {
    "allow client creation" in {
      val settings = MqttSessionSettings()
      val session = ActorMqttClientSession(settings)
      session.shutdown()
    }

    "allow server creation" in {
      val settings = MqttSessionSettings()
      val session = ActorMqttServerSession(settings)
      session.shutdown()
    }
  }

}
