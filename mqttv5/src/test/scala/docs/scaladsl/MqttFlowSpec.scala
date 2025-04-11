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

import scala.concurrent.Future
import scala.concurrent.Promise

import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttFlow
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttMessageWithAck
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

class MqttFlowSpec extends MqttSpecBase("MqttFlowSpec") {

  "mqtt flow" should {
    "establish a bidirectional connection and subscribe to a topic" in {
      val topic = "v5/flow-spec/topic"
      // #create-flow
      val mqttFlow: Flow[MqttMessage, MqttMessage, Future[Done]] =
        MqttFlow.atMostOnce(
          connectionSettings.withClientId("flow-spec/flow"),
          MqttSubscriptions(topic, MqttQoS.AtLeastOnce),
          bufferSize = 8,
          MqttQoS.AtLeastOnce)
      // #create-flow

      val source = Source.maybe[MqttMessage]

      // #run-flow
      val ((mqttMessagePromise, subscribed), result) = source
        .viaMat(mqttFlow)(Keep.both)
        .toMat(Sink.seq)(Keep.both)
        .run()
      // #run-flow

      subscribed.futureValue shouldBe Done
      mqttMessagePromise.success(None)
      noException should be thrownBy result.futureValue
    }

    "send an ack after sent confirmation" in {
      val topic = "v5/flow-spec/topic-ack"

      // #create-flow-ack
      val mqttFlow: Flow[MqttMessageWithAck, MqttMessageWithAck, Future[Done]] =
        MqttFlow.atLeastOnceWithAck(
          connectionSettings,
          MqttSubscriptions(topic, MqttQoS.AtLeastOnce),
          bufferSize = 8,
          MqttQoS.AtLeastOnce)
      // #create-flow-ack

      val acked = Promise[Done]()

      class MqttMessageWithAckFake extends MqttMessageWithAck {
        override val message: MqttMessage = MqttMessage.create(topic, ByteString.fromString("ohi"))

        override def ack(): Future[Done] = {
          acked.trySuccess(Done)
          Future.successful(Done)
        }
      }

      val message = new MqttMessageWithAckFake

      val source = Source.single(message)

      // #run-flow-ack
      val (subscribed, result) = source
        .viaMat(mqttFlow)(Keep.right)
        .toMat(Sink.seq)(Keep.both)
        .run()

      // #run-flow-ack
      subscribed.futureValue shouldBe Done
      result.futureValue shouldBe empty

      acked.future.futureValue shouldBe Done
    }
  }
}
