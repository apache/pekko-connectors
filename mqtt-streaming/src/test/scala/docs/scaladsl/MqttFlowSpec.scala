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

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.actor.ActorSystem
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.scaladsl.adapter._
import pekko.event.{ Logging, LoggingAdapter }
import pekko.stream.connectors.mqtt.streaming._
import pekko.stream.connectors.mqtt.streaming.scaladsl.{ ActorMqttClientSession, ActorMqttServerSession, Mqtt }
import pekko.stream.scaladsl.{ BroadcastHub, Flow, Keep, Sink, Source, SourceQueueWithComplete, Tcp }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.stream._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.testkit.TestKit
import pekko.util.ByteString
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UntypedMqttFlowSpec
    extends MqttFlowSpecBase("untyped-flow-spec/flow",
      "untyped-flow-spec/topic1",
      ActorSystem("UntypedMqttFlowSpec"))

class TypedMqttFlowSpec
    extends MqttFlowSpecBase("typed-flow-spec/flow",
      "typed-flow-spec/topic1",
      pekko.actor.typed.ActorSystem(Behaviors.ignore, "TypedMqttFlowSpec").toClassic)

abstract class MqttFlowSpecBase(clientId: String, topic: String, system: ActorSystem) extends TestKit(system)
    with AnyWordSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures with LogCapturing {

  override def sourceActorSytem = Some(system.name)

  private implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  private implicit val dispatcherExecutionContext: ExecutionContext = system.dispatcher

  private implicit val implicitSystem: ActorSystem = system

  implicit val logAdapter: LoggingAdapter = Logging(system, this.getClass.getName)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "mqtt client flow" should {
    "establish a bidirectional connection and subscribe to a topic" in assertAllStagesStopped {
      // #create-streaming-flow
      val settings = MqttSessionSettings()
      val session = ActorMqttClientSession(settings)

      val connection = Tcp().outgoingConnection("localhost", 1883)

      val mqttFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
        Mqtt
          .clientSessionFlow(session, ByteString("1"))
          .join(connection)
      // #create-streaming-flow

      // #run-streaming-flow
      val (commands: SourceQueueWithComplete[Command[Nothing]], events: Future[Publish]) =
        Source
          .queue(2, OverflowStrategy.fail)
          .via(mqttFlow)
          .collect {
            case Right(Event(p: Publish, _)) => p
          }
          .toMat(Sink.head)(Keep.both)
          .run()

      commands.offer(Command(Connect(clientId, ConnectFlags.CleanSession)))
      commands.offer(Command(Subscribe(topic)))
      session ! Command(
        Publish(ControlPacketFlags.RETAIN | ControlPacketFlags.QoSAtLeastOnceDelivery, topic, ByteString("ohi")))
      // #run-streaming-flow

      events.futureValue match {
        case Publish(_, `topic`, _, bytes) => bytes shouldBe ByteString("ohi")
        case e                             => fail("Unexpected event: " + e)
      }

      // #run-streaming-flow

      // for shutting down properly
      commands.complete()
      commands.watchCompletion().foreach(_ => session.shutdown())
      // #run-streaming-flow
    }
  }

  "mqtt server flow" should {
    // Ignored due to ://github.com/akka/alpakka/issues/1549, possibly
    // fixed with https://github.com/akka/alpakka/pull/2189
    "receive a bidirectional connection and a subscription to a topic" ignore {

      val host = "localhost"

      // #create-streaming-bind-flow
      val settings = MqttSessionSettings()
      val session = ActorMqttServerSession(settings)

      val maxConnections = 1

      val bindSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], Future[Tcp.ServerBinding]] =
        Tcp()
          .bind(host, 0)
          .flatMapMerge(
            maxConnections,
            { connection =>
              val mqttFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
                Mqtt
                  .serverSessionFlow(session, ByteString(connection.remoteAddress.getAddress.getAddress))
                  .join(connection.flow)

              val (queue, source) = Source
                .queue[Command[Nothing]](3, OverflowStrategy.dropHead)
                .via(mqttFlow)
                .toMat(BroadcastHub.sink)(Keep.both)
                .run()

              val subscribed = Promise[Done]()
              source
                .runForeach {
                  case Right(Event(_: Connect, _)) =>
                    queue.offer(Command(ConnAck(ConnAckFlags.None, ConnAckReturnCode.ConnectionAccepted)))
                  case Right(Event(cp: Subscribe, _)) =>
                    queue.offer(Command(SubAck(cp.packetId, cp.topicFilters.map(_._2)), Some(subscribed), None))
                  case Right(Event(publish @ Publish(flags, _, Some(packetId), _), _))
                      if flags.contains(ControlPacketFlags.RETAIN) =>
                    queue.offer(Command(PubAck(packetId)))
                    subscribed.future.foreach(_ => session ! Command(publish))
                  case _ => // Ignore everything else
                }

              source
            })
      // #create-streaming-bind-flow

      // #run-streaming-bind-flow
      val (bound: Future[Tcp.ServerBinding], server: UniqueKillSwitch) = bindSource
        .viaMat(KillSwitches.single)(Keep.both)
        .to(Sink.ignore)
        .run()
      // #run-streaming-bind-flow

      val binding = bound.futureValue
      binding.localAddress.getPort should not be 0

      val clientSession = ActorMqttClientSession(settings)
      val connection = Tcp().outgoingConnection(host, binding.localAddress.getPort)
      val mqttFlow = Mqtt.clientSessionFlow(clientSession, ByteString("1")).join(connection)
      val (commands, events) =
        Source
          .queue(2, OverflowStrategy.fail)
          .via(mqttFlow)
          .log("received")
          .collect {
            case Right(Event(p: Publish, _)) => p
          }
          .toMat(Sink.head)(Keep.both)
          .run()

      commands.offer(Command(Connect(clientId, ConnectFlags.None)))
      commands.offer(Command(Subscribe(topic)))
      clientSession ! Command(
        Publish(ControlPacketFlags.RETAIN | ControlPacketFlags.QoSAtLeastOnceDelivery, topic, ByteString("ohi")))

      events.futureValue match {
        case Publish(_, `topic`, _, bytes) => bytes shouldBe ByteString("ohi")
        case e                             => fail("Unexpected event: " + e)
      }
      // #run-streaming-bind-flow

      // for shutting down properly
      server.shutdown()
      session.shutdown()
      // #run-streaming-bind-flow
      commands.watchCompletion().foreach(_ => clientSession.shutdown())
    }
  }
}
