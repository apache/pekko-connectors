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

package org.apache.pekko.stream.connectors.ironmq.impl

import org.apache.pekko
import pekko.stream.connectors.ironmq.{ IronMqSettings, IronMqSpec, PushMessage }
import pekko.stream.scaladsl._
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped

import scala.concurrent.ExecutionContext

class IronMqPushStageSpec extends IronMqSpec {

  import ExecutionContext.Implicits.global

  "IronMqPushMessageStage" should {
    "push messages to the queue" in assertAllStagesStopped {

      val queueName = givenQueue()
      val flow = Flow.fromGraph(new IronMqPushStage(queueName, IronMqSettings()))

      val expectedMessagesBodies = List("test-1", "test-2")

      val producedMessagesIds = Source(expectedMessagesBodies)
        .map(PushMessage(_))
        .via(flow)
        .mapAsync(2)(identity)
        .mapConcat(_.ids)
        .toMat(Sink.seq)(Keep.right)
        .run()
        .futureValue

      val consumedMessagesIds = ironMqClient.pullMessages(queueName, 20).futureValue.map(_.messageId).toSeq

      consumedMessagesIds should contain theSameElementsAs producedMessagesIds
    }
  }

}
