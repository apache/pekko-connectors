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

package akka.stream.alpakka.ironmq.impl

import akka.stream.alpakka.ironmq.{ IronMqSettings, IronMqSpec, PushMessage }
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped

import scala.concurrent.ExecutionContext.Implicits.global

class IronMqPullStageSpec extends IronMqSpec {

  "IronMqSourceStage" when {
    "there are messages" should {
      "consume all messages" in assertAllStagesStopped {
        val queueName = givenQueue()
        val messages = (1 to 100).map(i => PushMessage(s"test-$i"))
        ironMqClient.pushMessages(queueName, messages: _*).futureValue

        val source = Source.fromGraph(new IronMqPullStage(queueName, IronMqSettings()))
        val receivedMessages = source.take(100).runWith(Sink.seq).map(_.map(_.message.body)).futureValue
        val expectedMessages = messages.map(_.body)

        receivedMessages should contain theSameElementsInOrderAs expectedMessages
      }
    }
  }

}
