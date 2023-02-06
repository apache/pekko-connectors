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

import org.apache.pekko.stream.connectors.ironmq.PushMessage
import org.apache.pekko.stream.connectors.ironmq.impl.IronMqClientForTests
import org.apache.pekko.stream.connectors.ironmq.scaladsl._
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.{ Flow, Sink, Source }
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.apache.pekko.testkit.TestKit
import org.apache.pekko.{ Done, NotUsed }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IronMqDocsSpec
    extends AnyWordSpec
    with IronMqClientForTests
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with LogCapturing {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(5.seconds, 250.millis)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "IronMqConsumer" should {
    "read messages" in assertAllStagesStopped {
      val queueName = givenQueue().futureValue
      import org.apache.pekko.stream.connectors.ironmq.PushMessage

      val messages = (1 to 100).map(i => s"test-$i")
      val produced = Source(messages)
        .map(PushMessage(_))
        .runWith(IronMqProducer.sink(queueName, ironMqSettings))
      produced.futureValue shouldBe Done

      // #atMostOnce
      import org.apache.pekko.stream.connectors.ironmq.Message

      val source: Source[Message, NotUsed] =
        IronMqConsumer.atMostOnceSource(queueName, ironMqSettings)

      val receivedMessages: Future[immutable.Seq[Message]] = source
        .take(100)
        .runWith(Sink.seq)
      // #atMostOnce
      receivedMessages.futureValue.map(_.body) should contain theSameElementsInOrderAs messages
    }

    "read messages and allow committing" in assertAllStagesStopped {
      val queueName = givenQueue().futureValue

      import org.apache.pekko.stream.connectors.ironmq.PushMessage
      val messages = (1 to 100).map(i => s"test-$i")
      val produced = Source(messages)
        .map(PushMessage(_))
        .runWith(IronMqProducer.sink(queueName, ironMqSettings))
      produced.futureValue shouldBe Done

      // #atLeastOnce
      import org.apache.pekko.stream.connectors.ironmq.scaladsl.CommittableMessage
      import org.apache.pekko.stream.connectors.ironmq.Message

      val source: Source[CommittableMessage, NotUsed] =
        IronMqConsumer.atLeastOnceSource(queueName, ironMqSettings)

      val businessLogic: Flow[CommittableMessage, CommittableMessage, NotUsed] =
        Flow[CommittableMessage] // do something useful with the received messages

      val receivedMessages: Future[immutable.Seq[Message]] = source
        .take(100)
        .via(businessLogic)
        .mapAsync(1)(m => m.commit().map(_ => m.message))
        .runWith(Sink.seq)
      // #atLeastOnce
      receivedMessages.futureValue.map(_.body) should contain theSameElementsInOrderAs messages

    }
  }

  "IronMqProducer" should {
    "push messages in flow" in assertAllStagesStopped {
      val queueName = givenQueue().futureValue

      val messageCount = 10
      // #flow
      import org.apache.pekko.stream.connectors.ironmq.{ Message, PushMessage }

      val messages: immutable.Seq[String] = (1 to messageCount).map(i => s"test-$i")
      val producedIds: Future[immutable.Seq[Message.Id]] = Source(messages)
        .map(PushMessage(_))
        .via(IronMqProducer.flow(queueName, ironMqSettings))
        .runWith(Sink.seq)
      // #flow
      producedIds.futureValue.size shouldBe messageCount

      val receivedMessages: Future[immutable.Seq[Message]] = IronMqConsumer
        .atMostOnceSource(queueName, ironMqSettings)
        .take(messages.size)
        .runWith(Sink.seq)
      receivedMessages.futureValue.map(_.body) should contain theSameElementsInOrderAs messages

    }

    "push messages at-least-once" in assertAllStagesStopped {
      val sourceQueue = givenQueue().futureValue
      val targetQueue = givenQueue().futureValue

      val messageCount = 10
      val messages: immutable.Seq[String] = (1 to messageCount).map(i => s"test-$i")
      val produced = Source(messages)
        .map(PushMessage(_))
        .runWith(IronMqProducer.sink(sourceQueue, ironMqSettings))
      produced.futureValue shouldBe Done

      // #atLeastOnceFlow
      import org.apache.pekko.stream.connectors.ironmq.{ Message, PushMessage }
      import org.apache.pekko.stream.connectors.ironmq.scaladsl.Committable

      val pushAndCommit: Flow[(PushMessage, Committable), Message.Id, NotUsed] =
        IronMqProducer.atLeastOnceFlow(targetQueue, ironMqSettings)

      val producedIds: Future[immutable.Seq[Message.Id]] = IronMqConsumer
        .atLeastOnceSource(sourceQueue, ironMqSettings)
        .take(messages.size)
        .map { committableMessage =>
          (PushMessage(committableMessage.message.body), committableMessage)
        }
        .via(pushAndCommit)
        .runWith(Sink.seq)
      // #atLeastOnceFlow
      producedIds.futureValue.size shouldBe messageCount

      val receivedMessages: Future[immutable.Seq[Message]] = IronMqConsumer
        .atMostOnceSource(targetQueue, ironMqSettings)
        .take(messages.size)
        .runWith(Sink.seq)
      receivedMessages.futureValue.map(_.body) should contain theSameElementsInOrderAs messages

    }

    "push messages to a sink" in assertAllStagesStopped {
      val queueName = givenQueue().futureValue

      val messageCount = 10
      // #sink
      import org.apache.pekko.stream.connectors.ironmq.{ Message, PushMessage }

      val messages: immutable.Seq[String] = (1 to messageCount).map(i => s"test-$i")
      val producedIds: Future[Done] = Source(messages)
        .map(PushMessage(_))
        .runWith(IronMqProducer.sink(queueName, ironMqSettings))
      // #sink
      producedIds.futureValue shouldBe Done

      val receivedMessages: Future[immutable.Seq[Message]] = IronMqConsumer
        .atMostOnceSource(queueName, ironMqSettings)
        .take(messages.size)
        .runWith(Sink.seq)
      receivedMessages.futureValue.map(_.body) should contain theSameElementsInOrderAs messages

    }
  }
}
