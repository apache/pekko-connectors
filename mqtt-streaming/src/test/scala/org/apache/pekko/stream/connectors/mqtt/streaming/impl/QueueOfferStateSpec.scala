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

package org.apache.pekko.stream.connectors.mqtt.streaming.impl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.testkit.typed.scaladsl.{ ActorTestKit, BehaviorTestKit, TestInbox }
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.connectors.mqtt.streaming.impl.QueueOfferState.QueueOfferCompleted
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class QueueOfferStateSpec
    extends TestKit(ActorSystem("QueueOfferStateSpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with LogCapturing {

  sealed trait Msg

  case class DoubleIt(n: Int, reply: ActorRef[Int]) extends Msg
  case object NotHandled extends Msg
  case class Done(result: Either[Throwable, QueueOfferResult]) extends Msg with QueueOfferCompleted

  private implicit val ec: ExecutionContext = system.dispatcher

  private val baseBehavior = Behaviors.receivePartial[Msg] {
    case (context, DoubleIt(n, reply)) =>
      reply.tell(n * 2)

      Behaviors.same
  }

  "waitForQueueOfferCompleted" should {
    "work when immediately enqueued" in {
      val behavior = QueueOfferState.waitForQueueOfferCompleted[Msg](
        Future.successful(QueueOfferResult.Enqueued),
        r => Done(r.toEither),
        baseBehavior,
        Vector.empty)

      val testKit = BehaviorTestKit(behavior)

      val inbox = TestInbox[Int]()

      testKit.run(DoubleIt(2, inbox.ref))

      inbox.expectMessage(4)
    }

    "work when enqueued after some time" in {
      val done = Promise[QueueOfferResult]()

      val behavior = QueueOfferState.waitForQueueOfferCompleted[Msg](
        done.future,
        r => Done(r.toEither),
        baseBehavior,
        Vector.empty)

      val testKit = ActorTestKit()
      val actor = testKit.spawn(behavior)
      val probe = testKit.createTestProbe[Int]()

      actor ! DoubleIt(2, probe.ref)

      system.scheduler.scheduleOnce(500.millis) {
        done.success(QueueOfferResult.Enqueued)
      }

      probe.expectMessage(5.seconds, 4)
    }

    "work when unhandled" in {
      val done = Promise[QueueOfferResult]()

      val behavior = QueueOfferState.waitForQueueOfferCompleted[Msg](
        done.future,
        r => Done(r.toEither),
        baseBehavior,
        Vector.empty)

      val testKit = ActorTestKit()
      val actor = testKit.spawn(behavior)
      val probe = testKit.createTestProbe[Int]()

      actor ! NotHandled
      actor ! DoubleIt(4, probe.ref)

      system.scheduler.scheduleOnce(500.millis) {
        done.success(QueueOfferResult.Enqueued)
      }

      probe.expectMessage(8)
    }
  }
}
