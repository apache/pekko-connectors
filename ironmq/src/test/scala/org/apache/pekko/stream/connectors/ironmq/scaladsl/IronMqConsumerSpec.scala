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

package org.apache.pekko.stream.connectors.ironmq.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.dispatch.ExecutionContexts
import pekko.stream.connectors.ironmq.{ IronMqSettings, IronMqSpec, PushMessage }
import pekko.stream.scaladsl.{ Sink, Source }
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.ParallelTestExecution
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped

import scala.concurrent.ExecutionContext

class IronMqConsumerSpec extends IronMqSpec with ParallelTestExecution {

  implicit val ec: ExecutionContext = ExecutionContexts.global()

  val messages: Source[PushMessage, NotUsed] =
    Source.fromIterator(() => Iterator.from(0)).map(i => PushMessage(s"test-$i"))

  override protected def initConfig(): Config =
    ConfigFactory.parseString(s"""pekko.connectors.ironmq {
         |  consumer.reservation-timeout = 30 seconds
         |}
      """.stripMargin).withFallback(super.initConfig())

  "atLeastOnceConsumerSource" should {
    "not delete messages from the queue if not committed" in assertAllStagesStopped {
      val queue = givenQueue()
      val numberOfMessages = 10

      messages
        .take(numberOfMessages)
        .mapAsync(1)(ironMqClient.pushMessages(queue, _))
        .runWith(Sink.ignore)
        .futureValue

      IronMqConsumer
        .atLeastOnceSource(queue, IronMqSettings())
        .take(numberOfMessages)
        .runWith(Sink.ignore)
        .futureValue

      ironMqClient.peekMessages(queue, 100).futureValue shouldBe empty

      // Sleep enough time to be sure the messages has been put back in queue by IronMQ
      Thread.sleep(45000L)

      ironMqClient.peekMessages(queue, 100).futureValue should have size numberOfMessages
    }

    "delete the messages from the queue when committed" in assertAllStagesStopped {
      val queue = givenQueue()
      val numberOfMessages = 10

      messages
        .take(numberOfMessages)
        .mapAsync(1)(ironMqClient.pushMessages(queue, _))
        .runWith(Sink.ignore)
        .futureValue

      IronMqConsumer
        .atLeastOnceSource(queue, IronMqSettings())
        .take(numberOfMessages)
        .mapAsync(3)(_.commit())
        .runWith(Sink.ignore)
        .futureValue

      ironMqClient.peekMessages(queue, 100).futureValue shouldBe empty

      // Sleep enough time to be sure the messages may have been put back in queue by IronMQ
      Thread.sleep(45000L)

      ironMqClient.peekMessages(queue, 100).futureValue shouldBe empty
    }

  }

}
