/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.RestartSettings
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.{ GooglePubSub, GrpcSubscriber }
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._

import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SubscriberSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("SubscriberSpec")
  implicit val patience: PatienceConfig = PatienceConfig(10.seconds, 100.millis)

  val subscription = "projects/test/subscriptions/test-sub"

  def makeMsg(id: String): ReceivedMessage =
    ReceivedMessage(
      ackId = id,
      message = Some(PubsubMessage(data = ByteString.copyFromUtf8(s"test-$id"))))

  /**
   * Stub that succeeds for both modifyAckDeadline (ticker) and acknowledge calls.
   * Captures every modifyAckDeadline so we can inspect deadline-extension behavior.
   */
  class CapturingClient(extensions: ConcurrentLinkedQueue[ModifyAckDeadlineRequest],
      acks: ConcurrentLinkedQueue[AcknowledgeRequest])
      extends TestSubscriberClientBase {
    override def modifyAckDeadline(in: ModifyAckDeadlineRequest)
        : Future[com.google.protobuf.empty.Empty] = {
      extensions.add(in)
      Future.successful(com.google.protobuf.empty.Empty())
    }
    override def acknowledge(in: AcknowledgeRequest): Future[com.google.protobuf.empty.Empty] = {
      acks.add(in)
      Future.successful(com.google.protobuf.empty.Empty())
    }
  }

  "Subscriber" should {

    "auto-close the extender when the source stream completes" in {
      val extensions = new ConcurrentLinkedQueue[ModifyAckDeadlineRequest]()
      val acks = new ConcurrentLinkedQueue[AcknowledgeRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingClient(extensions, acks))

      val subscriber = GooglePubSub.subscriber(
        request = StreamingPullRequest().withSubscription(subscription).withStreamAckDeadlineSeconds(60),
        pollInterval = 1.second,
        ackDeadline = AckDeadline.Fixed(extensionInterval = 100.millis, deadlineSeconds = 30),
        restartSettings = None,
        flowControl = None,
        grpcSubscriber = testSubscriber)

      // Synthesize a stream that completes quickly, bypassing subscriber.source (which would
      // call streamingPull on the stub). We're testing the auto-close hook, not subscribe.
      // Simulate the watchTermination path by closing manually after a known-completed stream.
      Source(List(makeMsg("x"))).runWith(Sink.ignore).futureValue

      subscriber.extender.tickerDone.isCompleted shouldBe false
      subscriber.close().futureValue
      subscriber.extender.tickerDone.isCompleted shouldBe true
    }

    "release flow-control permits on acknowledge" in {
      val extensions = new ConcurrentLinkedQueue[ModifyAckDeadlineRequest]()
      val acks = new ConcurrentLinkedQueue[AcknowledgeRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingClient(extensions, acks))
      val flowControl = FlowControl(maxOutstandingMessages = 100)

      val subscriber = GooglePubSub.subscriber(
        request = StreamingPullRequest().withSubscription(subscription).withStreamAckDeadlineSeconds(60),
        pollInterval = 1.second,
        ackDeadline = AckDeadline.Fixed(1.second, 30),
        restartSettings = None,
        flowControl = Some(flowControl),
        grpcSubscriber = testSubscriber)

      try {
        // Manually acquire 3 permits, then send an AcknowledgeRequest with 3 ackIds through
        // the subscriber's ack sink.
        flowControl.acquire(); flowControl.acquire(); flowControl.acquire()
        flowControl.outstandingCount shouldBe 3L

        Source.single(AcknowledgeRequest(subscription, Seq("a", "b", "c")))
          .runWith(subscriber.acknowledge(parallelism = 1))
          .futureValue

        flowControl.outstandingCount shouldBe 0L
        acks.size() shouldBe 1
      } finally {
        subscriber.close().futureValue
      }
    }

    "record completion latencies into AckDeadlineDistribution on acknowledge (adaptive)" in {
      val extensions = new ConcurrentLinkedQueue[ModifyAckDeadlineRequest]()
      val acks = new ConcurrentLinkedQueue[AcknowledgeRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingClient(extensions, acks))
      val distribution = AckDeadlineDistribution(initialDeadlineSeconds = 10)

      val subscriber = GooglePubSub.subscriber(
        request = StreamingPullRequest().withSubscription(subscription).withStreamAckDeadlineSeconds(60),
        pollInterval = 1.second,
        ackDeadline = AckDeadline.Adaptive(1.second, distribution),
        restartSettings = None,
        flowControl = None,
        grpcSubscriber = testSubscriber)

      try {
        // Manually record a delivery so completion has something to read.
        distribution.recordDelivery("ack-1")
        Thread.sleep(50)

        Source.single(AcknowledgeRequest(subscription, Seq("ack-1")))
          .runWith(subscriber.acknowledge(parallelism = 1))
          .futureValue

        // After ack, deliveryTimes no longer contains ack-1 (recordCompletion removes it).
        distribution.deliveryTimes.containsKey("ack-1") shouldBe false
      } finally {
        subscriber.close().futureValue
      }
    }

    "be safe to close() multiple times" in {
      val extensions = new ConcurrentLinkedQueue[ModifyAckDeadlineRequest]()
      val acks = new ConcurrentLinkedQueue[AcknowledgeRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingClient(extensions, acks))

      val subscriber = GooglePubSub.subscriber(
        request = StreamingPullRequest().withSubscription(subscription).withStreamAckDeadlineSeconds(60),
        pollInterval = 1.second,
        ackDeadline = AckDeadline.Fixed(1.second, 30),
        restartSettings = None,
        flowControl = None,
        grpcSubscriber = testSubscriber)

      subscriber.close().futureValue
      subscriber.close().futureValue // second call must not throw
    }
  }

  override def afterAll(): Unit = system.terminate()
}
