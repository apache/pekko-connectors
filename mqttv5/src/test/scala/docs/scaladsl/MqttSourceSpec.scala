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

import javax.net.ssl.SSLContext

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.stream._
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttMessageWithAck
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttSink
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttSource
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.apache.pekko.util.ByteString
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.slf4j.LoggerFactory

class MqttSourceSpec extends MqttSpecBase("MqttSourceSpec") {

  private val log = LoggerFactory.getLogger(classOf[MqttSourceSpec])

  private val topic1 = "v5/source-spec/topic1"

  private val sourceSettings = connectionSettings.withClientId("source-spec/source")
  private val sinkSettings = connectionSettings.withClientId("source-spec/sink")

  /**
   * Wrap a source with restart logic and exposes an equivalent materialized value.
   * Could be simplified when https://github.com/akka/akka/issues/24771 is solved.
   */
  def wrapWithRestart[M](
      source: => Source[M, Future[Done]])(implicit ec: ExecutionContext): Source[M, Future[Done]] = {
    val subscribed = Promise[Done]()
    RestartSource
      .withBackoff(
        RestartSettings(minBackoff = 100.millis, maxBackoff = 3.seconds, randomFactor = 0.2).withMaxRestarts(5,
          1.second)) { () =>
        source
          .mapMaterializedValue { f =>
            f.onComplete(res => subscribed.complete(res))
          }
      }
      .mapMaterializedValue(_ => subscribed.future)
  }

  "MQTT connection settings" should {
    "accept standard things" in {
      // #create-connection-settings
      val connectionSettings = MqttConnectionSettings(
        broker = "tcp://localhost:1883",
        "test-scala-client",
        persistence = new MemoryPersistence
      )
      // #create-connection-settings
      connectionSettings.toString should include("tcp://localhost:1883")
    }

    "allow SSL" in {
      // #ssl-settings
      val connectionSettings = MqttConnectionSettings(
        broker = "ssl://localhost:1885",
        "ssl-client",
        persistence = new MemoryPersistence).withAuth("mqttUser", "mqttPassword")
        .withSocketFactory(SSLContext.getDefault.getSocketFactory)
      // #ssl-settings
      connectionSettings.toString should include("ssl://localhost:1885")
      connectionSettings.asMqttConnectionOptions().getUserName should be("mqttUser")
    }

    "allow MQTT buffering offline support persistence" in {
      // #OfflinePersistenceSettings
      val bufferedConnectionSettings = MqttConnectionSettings(
        broker = "ssl://localhost:1885",
        "ssl-client",
        persistence = new MemoryPersistence
      ).withOfflinePersistenceSettings(
        bufferSize = 1234,
        deleteOldestMessage = true,
        persistBuffer = false
      )

      bufferedConnectionSettings.toString should include(
        "offlinePersistence=Some(MqttOfflinePersistenceSettings(1234,true,false))")
    }
  }

  "mqtt source" should {
    "consume unacknowledged messages from previous sessions using manualAck" in {
      import system.dispatcher

      val topic = "v5/source-spec/manualacks"
      val input = Vector("one", "two", "three", "four", "five")

      // #create-source-with-manualacks
      val mqttSource: Source[MqttMessageWithAck, Future[Done]] =
        MqttSource.atLeastOnce(
          connectionSettings
            .withClientId("source-spec/source1")
            .withCleanStart(false),
          MqttSubscriptions(topic, MqttQoS.AtLeastOnce),
          bufferSize = 8)
      // #create-source-with-manualacks

      val (subscribed, unackedResult) = mqttSource.take(input.size).toMat(Sink.seq)(Keep.both).run()
      val mqttSink = MqttSink(sinkSettings, MqttQoS.AtLeastOnce)

      Await.ready(subscribed, timeout)
      Source(input).map(item => MqttMessage(topic, ByteString(item))).runWith(mqttSink)

      unackedResult.futureValue.map(message => message.message.payload.utf8String) should equal(input)

      val businessLogic: Flow[MqttMessageWithAck, MqttMessageWithAck, NotUsed] = Flow[MqttMessageWithAck]

      // #run-source-with-manualacks
      val result = mqttSource
        .via(businessLogic)
        .mapAsync(1)(messageWithAck => messageWithAck.ack().map(_ => messageWithAck.message))
        .take(input.size)
        .runWith(Sink.seq)
      // #run-source-with-manualacks
      result.futureValue.map(message => message.payload.utf8String) should equal(input)
    }

    "keep connection open if downstream closes and there are pending acks" in {
      val topic = "v5/source-spec/pendingacks"
      val input = Vector("one", "two", "three", "four", "five")

      val connectionSettings = sourceSettings.withCleanStart(false)
      val subscriptions = MqttSubscriptions(topic, MqttQoS.AtLeastOnce)
      val mqttSource = MqttSource.atLeastOnce(connectionSettings, subscriptions, 8)

      val (subscribed, unackedResult) = mqttSource.take(input.size).toMat(Sink.seq)(Keep.both).run()
      val mqttSink = MqttSink(sinkSettings, MqttQoS.AtLeastOnce)

      Await.ready(subscribed, timeout)
      Source(input).map(item => MqttMessage(topic, ByteString(item))).runWith(mqttSink).futureValue shouldBe Done

      unackedResult.futureValue.map(msg => {
        noException should be thrownBy msg.ack().futureValue
      })
    }

    "receive a message from a topic" in {
      val msg = MqttMessage(topic1, ByteString("ohi"))

      val subscriptions = MqttSubscriptions(topic1, MqttQoS.AtLeastOnce)
      val (subscribed, result) = MqttSource
        .atMostOnce(sourceSettings, subscriptions, 8)
        .toMat(Sink.head)(Keep.both)
        .run()

      Await.ready(subscribed, timeout)
      Source.single(msg).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))

      result.futureValue shouldBe msg
    }

    "receive messages from multiple topics" in {
      val topic2 = "v5/source-spec/topic2"
      val messages = (0 until 7)
        .flatMap(i =>
          Seq(
            MqttMessage(topic1, ByteString(s"ohi_$i")),
            MqttMessage(topic2, ByteString(s"ohi_$i"))))

      // #create-source
      val mqttSource: Source[MqttMessage, Future[Done]] =
        MqttSource.atMostOnce(
          connectionSettings.withClientId("source-spec/source"),
          MqttSubscriptions(Map(topic1 -> MqttQoS.AtLeastOnce, topic2 -> MqttQoS.AtLeastOnce)),
          bufferSize = 8)

      val (subscribed, streamResult) = mqttSource
        .take(messages.size)
        .toMat(Sink.seq)(Keep.both)
        .run()
      // #create-source

      Await.ready(subscribed, timeout)
      // #run-sink
      val sink: Sink[MqttMessage, Future[Done]] =
        MqttSink(connectionSettings, MqttQoS.AtLeastOnce)
      Source(messages).runWith(sink)
      // #run-sink

      streamResult.futureValue shouldBe messages
    }

    "connection should fail to wrong broker" in {
      val wrongConnectionSettings = connectionSettings.withBroker("tcp://localhost:1884")

      val (subscribed, _) = MqttSource
        .atMostOnce(wrongConnectionSettings, MqttSubscriptions(topic1, MqttQoS.atLeastOnce), 8)
        .toMat(Sink.head)(Keep.both)
        .run()

      subscribed.failed.futureValue shouldBe an[MqttException]
    }

    "fail connection when not providing the requested credentials" in {
      val secureTopic = "v5/source-spec/secure-topic1"
      val first = MqttSource
        .atMostOnce(sourceSettings.withAuth("username1", "bad_password"),
          MqttSubscriptions(secureTopic, MqttQoS.AtLeastOnce),
          8)
        .runWith(Sink.head)

      whenReady(first.failed) {
        case e: MqttException => e.getMessage should include("Not authorized")
        case e                => throw e
      }
    }

    "receive a message from a topic with right credentials" in {
      val secureTopic = "v5/source-spec/secure-topic2"
      val msg = MqttMessage(secureTopic, ByteString("ohi"))

      val (subscribed, result) = MqttSource
        .atMostOnce(sourceSettings.withAuth("username1", "password1"),
          MqttSubscriptions(secureTopic, MqttQoS.AtLeastOnce),
          8)
        .toMat(Sink.head)(Keep.both)
        .run()

      Await.ready(subscribed, timeout)
      Source.single(msg).runWith(MqttSink(sinkSettings.withAuth("username1", "password1"), MqttQoS.AtLeastOnce))

      result.futureValue shouldBe msg
    }

    "signal backpressure" in {
      val bufferSize = 8
      val overflow = 4
      val messages = (1 until bufferSize + overflow)
        .map(i => s"ohi_$i")

      val (subscribed, result) = MqttSource
        .atMostOnce(sourceSettings, MqttSubscriptions(topic1, MqttQoS.AtLeastOnce), bufferSize)
        .take(messages.size)
        .toMat(Sink.seq)(Keep.both)
        .run()

      Await.ready(subscribed, timeout)
      Source(messages)
        .map(m => MqttMessage(topic1, ByteString(m)))
        .runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))

      result.futureValue.map(m => m.payload.utf8String) shouldBe messages
    }

    "work with fast downstream" in {
      val bufferSize = 8
      val overflow = 4
      val messages = (1 until bufferSize + overflow)
        .map(i => s"ohi_$i")

      val (subscribed, result) = MqttSource
        .atMostOnce(sourceSettings, MqttSubscriptions(topic1, MqttQoS.AtLeastOnce), bufferSize)
        .take(messages.size)
        .toMat(Sink.seq)(Keep.both)
        .run()

      Await.ready(subscribed, timeout)
      Source(messages)
        .map(m => MqttMessage(topic1, ByteString(m)))
        .runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))

      result.futureValue.map(m => m.payload.utf8String) shouldBe messages
    }

    "support multiple materialization" in {
      val source = MqttSource.atMostOnce(sourceSettings, MqttSubscriptions(topic1, MqttQoS.AtLeastOnce), 8)

      val (subscribed, elem) = source.toMat(Sink.head)(Keep.both).run()

      Await.ready(subscribed, timeout)
      Source.single(MqttMessage(topic1, ByteString("ohi"))).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))
      elem.futureValue shouldBe MqttMessage(topic1, ByteString("ohi"))

      val (subscribed2, elem2) = source.toMat(Sink.head)(Keep.both).run()

      Await.ready(subscribed2, timeout)
      Source.single(MqttMessage(topic1, ByteString("ohi"))).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))
      elem2.futureValue shouldBe MqttMessage(topic1, ByteString("ohi"))
    }

    "automatically reconnect" in {
      import system.dispatcher

      val msg = MqttMessage(topic1, ByteString("ohi"))

      // Create a proxy on an available port so it can be shut down
      val (proxyBinding, connection) = Tcp().bind("localhost", 0).toMat(Sink.head)(Keep.both).run()
      val proxyPort = proxyBinding.futureValue.localAddress.getPort
      val proxyKs = connection.map { c =>
        c.handleWith(
          Tcp()
            .outgoingConnection("localhost", 1883)
            .viaMat(KillSwitches.single)(Keep.right))
      }
      Await.ready(proxyBinding, timeout)

      val (subscribed, probe) = MqttSource
        .atMostOnce(
          sourceSettings
            .withAutomaticReconnect(true)
            .withCleanStart(false)
            .withBroker(s"tcp://localhost:$proxyPort"),
          MqttSubscriptions(topic1, MqttQoS.AtLeastOnce),
          8)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      // Ensure that the connection made it all the way to the server by waiting until it receives a message
      Await.ready(subscribed, timeout)

      Source.single(msg).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))

      try {
        probe.requestNext()
      } catch {
        case e: Exception =>
          log.debug(s"Ignoring $e", e)
      }
      // Kill the proxy, producing an unexpected disconnection of the client
      Await.result(proxyKs, timeout).shutdown()

      // Restart the proxy
      val (proxyBinding2, connection2) = Tcp().bind("localhost", proxyPort).toMat(Sink.head)(Keep.both).run()
      val proxyKs2 = connection2.map { c =>
        c.handleWith(
          Tcp()
            .outgoingConnection("localhost", 1883)
            .viaMat(KillSwitches.single)(Keep.right))
      }
      Await.ready(proxyBinding2, timeout)

      Source.single(msg).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))
      probe.requestNext(5.seconds) shouldBe msg
      Await.result(proxyKs2, timeout).shutdown()
    }

    "support will message" in {
      import system.dispatcher

      val willTopic = "v5/source-spec/will"
      val msg = MqttMessage(topic1, ByteString("ohi"))

      // #will-message
      val lastWill = MqttMessage(willTopic, ByteString("ohi"))
        .withQos(MqttQoS.AtLeastOnce)
        .withRetained(true)
      // #will-message

      // Create a proxy on an available port so it can be shut down
      val (proxyBinding, connection) = Tcp().bind("localhost", 0).toMat(Sink.head)(Keep.both).run()
      val proxyPort = proxyBinding.futureValue.localAddress.getPort
      val proxyKs = connection.map { c =>
        c.handleWith(
          Tcp()
            .outgoingConnection("localhost", 1883)
            .viaMat(KillSwitches.single)(Keep.right))
      }
      Await.ready(proxyBinding, timeout)

      val source1 = wrapWithRestart(
        MqttSource
          .atMostOnce(
            sourceSettings
              .withClientId("source-spec/testator")
              .withBroker(s"tcp://localhost:$proxyPort")
              .withWill(lastWill),
            MqttSubscriptions(topic1, MqttQoS.AtLeastOnce),
            8))

      val (subscribed, probe) = source1.toMat(TestSink.probe)(Keep.both).run()

      // Ensure that the connection made it all the way to the server by waiting until it receives a message
      Await.ready(subscribed, timeout)
      Source.single(msg).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))
      try {
        probe.requestNext()
      } catch {
        case e: Exception =>
          log.debug(s"Ignoring $e", e)
      }

      // Kill the proxy, producing an unexpected disconnection of the client
      Await.result(proxyKs, timeout).shutdown()

      val source2 = MqttSource.atMostOnce(sourceSettings.withClientId("source-spec/executor"),
        MqttSubscriptions(willTopic, MqttQoS.AtLeastOnce),
        8)

      val elem = source2.runWith(Sink.head)
      elem.futureValue shouldBe MqttMessage(willTopic, ByteString("ohi"))
    }

    "support buffering message on disconnect" in {
      import system.dispatcher

      val msg = MqttMessage(topic1, ByteString("ohi"))

      val sharedKillSwitch = KillSwitches.shared("buffered-test-kill-switch")

      // Create a proxy on an available port so it can be shut down
      val (proxyBinding, connection) = Tcp().bind("localhost", 0).toMat(Sink.head)(Keep.both).run()
      val proxyPort = proxyBinding.futureValue.localAddress.getPort
      connection.map { c =>
        c.handleWith(
          Tcp()
            .outgoingConnection("localhost", 1883)
            .via(sharedKillSwitch.flow))
      }
      Await.ready(proxyBinding, timeout)

      val (killSwitch, probe) = MqttSource
        .atMostOnce(
          sourceSettings
            .withCleanStart(false)
            .withBroker(s"tcp://localhost:$proxyPort")
            .withOfflinePersistenceSettings(bufferSize = 1234),
          MqttSubscriptions(topic1, MqttQoS.AtLeastOnce),
          8)
        .via(sharedKillSwitch.flow)
        .toMat(TestSink.probe)(Keep.both)
        .run()
      Await.ready(killSwitch, timeout)

      Source.single(msg).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))
      try {
        probe.requestNext()
      } catch {
        case e: Exception =>
          log.debug(s"Ignoring $e", e)
      }
      // Kill the proxy and stream
      sharedKillSwitch.shutdown()

      // Send message with connection and stream down
      Source.single(msg).runWith(MqttSink(sinkSettings, MqttQoS.AtLeastOnce))

      // Restart the proxy
      val (proxyBinding2, connection2) = Tcp().bind("localhost", proxyPort).toMat(Sink.head)(Keep.both).run()
      val proxyKs2 = connection2.map { c =>
        c.handleWith(
          Tcp()
            .outgoingConnection("localhost", 1883)
            .viaMat(KillSwitches.single)(Keep.right))
      }
      Await.ready(proxyBinding2, timeout)

      // Rebuild MQTT connection to broker
      val (subscribed, probe2) = MqttSource
        .atMostOnce(
          sourceSettings
            .withCleanStart(false)
            .withBroker(s"tcp://localhost:$proxyPort")
            .withOfflinePersistenceSettings(bufferSize = 1234),
          MqttSubscriptions(topic1, MqttQoS.AtLeastOnce),
          8)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      // Ensure that the connection made it all the way to the server by waiting until it receives a message
      Await.ready(subscribed, timeout)

      probe2.requestNext(5.seconds) shouldBe msg
      Await.result(proxyKs2, timeout).shutdown()
    }
  }
}
