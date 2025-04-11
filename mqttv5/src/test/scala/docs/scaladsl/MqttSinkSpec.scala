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

import scala.concurrent.Await
import scala.concurrent.duration._

import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.mqttv5
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttSink
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttSource
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.eclipse.paho.mqttv5.common.MqttException

class MqttSinkSpec extends MqttSpecBase("MqttSinkSpec") {
  "mqtt sink" should {
    "send one message to a topic" in {
      val topic = "v5/sink-spec/topic1"

      val msg = MqttMessage(topic, ByteString("ohi"))

      val (subscribed, message) = MqttSource
        .atMostOnce(connectionSettings.withClientId("sink-spec/source1"),
          MqttSubscriptions(topic, MqttQoS.AtLeastOnce),
          8)
        .toMat(Sink.head)(Keep.both)
        .run()

      Await.ready(subscribed, timeout)
      Source.single(msg).runWith(MqttSink(connectionSettings.withClientId("sink-spec/sink1"),
        MqttQoS.atLeastOnce))

      message.futureValue shouldBe msg
    }

    "send multiple messages to a topic" in {
      val topic = "v5/sink-spec/topic2"

      val msg = MqttMessage(topic, ByteString("ohi"))
      val numOfMessages = 5

      val (subscribed, messagesFuture) =
        MqttSource
          .atMostOnce(connectionSettings.withClientId("sink-spec/source2"),
            MqttSubscriptions(topic, MqttQoS.atLeastOnce),
            8)
          .take(numOfMessages)
          .toMat(Sink.seq)(Keep.both)
          .run()

      Await.ready(subscribed, timeout)
      Source(1 to numOfMessages).map(_ => msg).runWith(
        MqttSink(connectionSettings.withClientId("sink-spec/sink2"), MqttQoS.atLeastOnce))

      val messages = messagesFuture.futureValue
      (messages should have).length(numOfMessages)
      messages.foreach { _ shouldBe msg }
    }

    "connection should fail to wrong broker" in {
      val secureTopic = "v5/sink-spec/secure-topic1"

      val wrongConnectionSettings =
        connectionSettings.withClientId("sink-spec/sink3").withBroker("tcp://localhost:1884")
      val msg = MqttMessage(secureTopic, ByteString("ohi"))

      val termination = Source
        .single(msg)
        .runWith(MqttSink(wrongConnectionSettings, MqttQoS.atLeastOnce))

      termination.failed.futureValue shouldBe an[MqttException]
    }

    "fail to publish when credentials are not provided" in {
      val secureTopic = "v5/sink-spec/secure-topic2"
      val msg = MqttMessage(secureTopic, ByteString("ohi"))

      val termination =
        Source.single(msg).runWith(MqttSink(connectionSettings.withClientId("sink-spec/sink4").withAuth(
            "username1", "bad_password"), MqttQoS.atLeastOnce))

      whenReady(termination.failed) { ex =>
        ex shouldBe an[MqttException]
        ex.getMessage should include("Not authorized")
      }
    }

    "publish when credentials are provided" in {
      val secureTopic = "v5/sink-spec/secure-topic3"
      val msg = MqttMessage(secureTopic, ByteString("ohi"))

      val (subscribed, message) = MqttSource
        .atMostOnce(connectionSettings.withClientId("sink-spec/source1").withAuth("username1",
          "password1"),
          MqttSubscriptions(secureTopic, MqttQoS.AtLeastOnce),
          8)
        .toMat(Sink.head)(Keep.both)
        .run()

      Await.ready(subscribed, timeout)

      val termination = Source
        .single(msg)
        .runWith(MqttSink(connectionSettings.withClientId("sink-spec/sink5").withAuth("username1",
          "password1"), MqttQoS.atLeastOnce))

      termination.futureValue shouldBe Done

      message.futureValue shouldBe msg
    }

    "received retained message on new client" in {
      val topic = "v5/sink-spec/topic3"
      val msg = MqttMessage(topic, ByteString("ohi")).withQos(MqttQoS.atLeastOnce).withRetained(true)

      val messageSent = Source.single(msg).runWith(
        MqttSink(connectionSettings.withClientId("sink-spec/sink6"), MqttQoS.atLeastOnce))

      Await.ready(messageSent, 3.seconds)

      val messageFuture =
        MqttSource
          .atMostOnce(connectionSettings.withClientId("source-spec/retained"),
            mqttv5.MqttSubscriptions(topic, MqttQoS.atLeastOnce),
            8)
          .runWith(Sink.head)

      val message = messageFuture.futureValue
      message.topic shouldBe msg.topic
      message.payload shouldBe msg.payload
    }

    "fail to publish to an unauthorized topic" in {
      val clientId = "sink-spec/sink7"
      val topic = "v5/sink-spec/unauthorized"

      val msg = MqttMessage(topic, ByteString("ohi"))
      val numOfMessages = 5

      val termination = Source(1 to numOfMessages).map(_ => msg).runWith(
        MqttSink(connectionSettings.withClientId(clientId), MqttQoS.atLeastOnce))

      whenReady(termination.failed) { ex =>
        ex shouldBe an[RuntimeException]
        ex.getMessage should be(
          s"Client [$clientId] received one or more error codes while publishing on topic [$topic] " +
          s"to broker [${connectionSettings.broker}]: [code=135]")
      }
    }
  }
}
