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
import pekko.{ Done, NotUsed }
import pekko.actor.ActorSystem
import pekko.stream.scaladsl.{ Keep, Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.{ AckDeadlineExtender, FlowControl }
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.{ GooglePubSub, GrpcSubscriber, PubSubAttributes }
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._
import org.apache.pekko.stream._

import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec

class AutoExtendAckDeadlinesSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  implicit val system: ActorSystem = ActorSystem("AutoExtendAckDeadlinesSpec")
  implicit val patience: PatienceConfig = PatienceConfig(10.seconds, 100.millis)

  val subscription = "projects/test/subscriptions/test-sub"

  def makeMsg(id: String): ReceivedMessage =
    ReceivedMessage(
      ackId = id,
      message = Some(PubsubMessage(data = ByteString.copyFromUtf8(s"test-$id"))))

  /** Stub that fails modifyAckDeadline calls. */
  class FailingModifyAckDeadlineClient extends TestSubscriberClientBase {
    override def modifyAckDeadline(
        in: ModifyAckDeadlineRequest): Future[com.google.protobuf.empty.Empty] =
      Future.failed(new RuntimeException("simulated modifyAckDeadline failure"))
  }

  /** Stub that succeeds for modifyAckDeadline calls. */
  class SucceedingClient extends TestSubscriberClientBase {
    override def modifyAckDeadline(
        in: ModifyAckDeadlineRequest): Future[com.google.protobuf.empty.Empty] =
      Future.successful(com.google.protobuf.empty.Empty())
  }

  /** Stub that captures every modifyAckDeadline request for inspection. */
  class CapturingClient(captured: ConcurrentLinkedQueue[ModifyAckDeadlineRequest])
      extends TestSubscriberClientBase {
    override def modifyAckDeadline(
        in: ModifyAckDeadlineRequest): Future[com.google.protobuf.empty.Empty] = {
      captured.add(in)
      Future.successful(com.google.protobuf.empty.Empty())
    }
  }

  "autoExtendAckDeadlines (fixed deadline)" should {

    "fail the main stream when the ticker's modifyAckDeadline fails" in {
      val testSubscriber = new GrpcSubscriber(new FailingModifyAckDeadlineClient())

      val result = Source(List(makeMsg("1"), makeMsg("2"), makeMsg("3")))
        .throttle(1, 500.millis)
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 200.millis, 30))
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.seq)

      val ex = result.failed.futureValue
      ex shouldBe an[AckDeadlineExtensionException]
      ex.getMessage should include("ticker failed")
      ex.getCause.getMessage should include("simulated modifyAckDeadline failure")
    }

    "fail an idle stream when the ticker dies after the last element" in {
      val testSubscriber = new GrpcSubscriber(new FailingModifyAckDeadlineClient())

      // Emit one message then idle — the ticker should abort the stream even
      // though no further elements arrive.
      val result = Source
        .single(makeMsg("only"))
        .concat(Source.maybe[ReceivedMessage]) // keeps the stream open indefinitely
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 200.millis, 30))
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.seq)

      val ex = result.failed.futureValue
      ex shouldBe an[AckDeadlineExtensionException]
      ex.getMessage should include("ticker failed")
    }

    "pass messages through when ticker is healthy" in {
      val testSubscriber = new GrpcSubscriber(new SucceedingClient())

      val result = Source(List(makeMsg("1"), makeMsg("2"), makeMsg("3")))
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 1.second, 30))
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.seq)

      result.futureValue should have size 3
      result.futureValue.map(_.ackId) shouldBe Seq("1", "2", "3")
    }

    "track all in-flight messages eagerly even when downstream is backpressured" in {
      // Regression test: with a plain `.map` tracker, only the one message held by mapAsync(1)
      // would be tracked at the first tick. The eager-pull stage must track all buffered
      // messages so the first ticker tick extends every in-flight ackId.
      val captured = new ConcurrentLinkedQueue[ModifyAckDeadlineRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingClient(captured))
      val n = 10

      val killSwitch = pekko.stream.KillSwitches.shared("test")
      val gate = Promise[ReceivedMessage]() // never completes — mapAsync(1) hangs forever

      Source(1 to n)
        .map(i => makeMsg(i.toString))
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 200.millis, 30))
        .via(killSwitch.flow)
        .mapAsync(1)(_ => gate.future)
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.ignore)

      // Wait for the first tick (200ms) plus jitter to fire.
      Thread.sleep(800)
      killSwitch.shutdown()

      val firstReq = captured.poll()
      firstReq should not be null
      firstReq.subscription shouldBe subscription
      firstReq.ackDeadlineSeconds shouldBe 30
      // All n ackIds must appear in the very first extension call. Without eager-pull,
      // this would only be 1 (the element currently grabbed by mapAsync).
      firstReq.ackIds.toSet shouldBe (1 to n).map(_.toString).toSet
    }
  }

  "autoExtendAckDeadlines (caller-owned AckDeadlineExtender)" should {

    "retain tracking state across stream materializations (restart-safety)" in {
      // Restart-safety regression test. The internal-ticker overload clears the tracking map
      // and cancels the ticker when the stream completes, so any messages received before a
      // RestartSource re-materialization lose extension coverage. The extender owns both the
      // map and the ticker, so extensions continue across stream completion / re-materialization.
      val captured = new ConcurrentLinkedQueue[ModifyAckDeadlineRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingClient(captured))
      val extender = AckDeadlineExtender(subscription, 200.millis, 30, 60.minutes, testSubscriber)(system)

      try {
        // First materialization: emit three messages then complete normally. The eager-pull
        // tracker writes them into the extender's tracked map.
        val streamOne = Source(List(makeMsg("a1"), makeMsg("a2"), makeMsg("a3")))
          .via(GooglePubSub.autoExtendAckDeadlines(extender))
          .runWith(Sink.ignore)
        streamOne.futureValue

        // The extender's map must outlive the stream — this is the contract that lets a
        // RestartSource-wrapped subscribe keep extension coverage during reconnect backoff.
        extender.trackedSize shouldBe 3

        // Let the ticker fire a few times. Each tick should carry all three ackIds.
        Thread.sleep(700)

        val allRequests = captured.iterator().asScala.toList
        val streamOneIds = Set("a1", "a2", "a3")
        val ticksWithStreamOneIds = allRequests.count(_.ackIds.exists(streamOneIds.contains))
        ticksWithStreamOneIds should be >= 2
        allRequests.filter(_.ackIds.exists(streamOneIds.contains)).foreach { req =>
          req.ackIds.toSet shouldBe streamOneIds
        }

        // Second materialization: simulate reconnect by running another stream. Tracking from
        // stream 1 must still be present, and stream 2 entries get added on top.
        val streamTwo = Source(List(makeMsg("b1"), makeMsg("b2")))
          .via(GooglePubSub.autoExtendAckDeadlines(extender))
          .runWith(Sink.ignore)
        streamTwo.futureValue
        extender.trackedSize shouldBe 5
      } finally {
        extender.close().futureValue
      }
    }
  }

  "GooglePubSub.flowControlGate" should {

    "acquire permits on receipt, not on push to downstream (eager-pull)" in {
      // Regression test: with the old downstream-demand-bound gate, only the message currently
      // held by mapAsync would acquire a permit. The eager-pull gate must count every message
      // it receives, so flowControl.outstandingCount climbs to the limit even when downstream
      // is fully saturated.
      val testSubscriber = new GrpcSubscriber(new SucceedingClient())
      val limit = 5
      val flowControl = FlowControl(maxOutstandingMessages = limit.toLong)
      val gate = Promise[ReceivedMessage]() // mapAsync(1) hangs forever
      val killSwitch = pekko.stream.KillSwitches.shared("flowControlGateTest")

      Source(1 to 100)
        .map(i => makeMsg(i.toString))
        .via(GooglePubSub.flowControlGate(flowControl))
        .via(killSwitch.flow)
        .mapAsync(1)(_ => gate.future)
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.ignore)

      // Give the gate time to fill up.
      Thread.sleep(300)

      // The gate should have admitted exactly `limit` messages: one currently held by mapAsync,
      // (limit - 1) buffered inside the gate. The Source(1 to 100) provides plenty of upstream
      // messages, but the gate stops pulling at the limit.
      // Without eager-pull this would be 1 (only the message in mapAsync had a permit acquired).
      flowControl.outstandingCount shouldBe limit.toLong

      killSwitch.shutdown()
    }
  }

  "autoExtendAckDeadlines (adaptive)" should {

    "fail the main stream when the ticker's modifyAckDeadline fails" in {
      val testSubscriber = new GrpcSubscriber(new FailingModifyAckDeadlineClient())
      val dist = AckDeadlineDistribution(initialDeadlineSeconds = 10)

      val result = Source(List(makeMsg("1"), makeMsg("2"), makeMsg("3")))
        .throttle(1, 500.millis)
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 200.millis, dist))
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.seq)

      val ex = result.failed.futureValue
      ex shouldBe an[AckDeadlineExtensionException]
      ex.getMessage should include("ticker failed")
    }

    "fail an idle stream when the ticker dies after the last element" in {
      val testSubscriber = new GrpcSubscriber(new FailingModifyAckDeadlineClient())
      val dist = AckDeadlineDistribution(initialDeadlineSeconds = 10)

      val result = Source
        .single(makeMsg("only"))
        .concat(Source.maybe[ReceivedMessage])
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 200.millis, dist))
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.seq)

      val ex = result.failed.futureValue
      ex shouldBe an[AckDeadlineExtensionException]
      ex.getMessage should include("ticker failed")
    }

    "pass messages through when ticker is healthy" in {
      val testSubscriber = new GrpcSubscriber(new SucceedingClient())
      val dist = AckDeadlineDistribution(initialDeadlineSeconds = 10)

      val result = Source(List(makeMsg("1"), makeMsg("2"), makeMsg("3")))
        .via(GooglePubSub.autoExtendAckDeadlines(subscription, 1.second, dist))
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .runWith(Sink.seq)

      result.futureValue should have size 3
    }
  }

  "GooglePubSub.subscribe" should {
    "send only allowed fields on subsequent StreamingPullRequest messages" in {
      // Regression test: the keepalive tick must NOT echo initial-only fields
      // (clientId, maxOutstandingMessages, maxOutstandingBytes) back to the server,
      // which would otherwise return INVALID_ARGUMENT on the first tick.
      val captured = new ConcurrentLinkedQueue[StreamingPullRequest]()
      val testSubscriber = new GrpcSubscriber(new CapturingStreamingPullClient(captured)(system))

      val initial = StreamingPullRequest()
        .withSubscription(subscription)
        .withStreamAckDeadlineSeconds(60)
        .withClientId("test-client")
        .withMaxOutstandingMessages(100L)
        .withMaxOutstandingBytes(10485760L)

      val cancellableFut = GooglePubSub
        .subscribe(initial, 100.millis)
        .withAttributes(PubSubAttributes.subscriber(testSubscriber))
        .toMat(Sink.ignore)(Keep.left)
        .run()

      // Wait until the initial request plus at least 2 keepalive ticks have landed,
      // then cancel.
      eventually(timeout(Span(5, Seconds)), interval(Span(50, Millis))) {
        captured.size should be >= 3
      }
      cancellableFut.futureValue.cancel()

      val first = captured.poll()
      first should not be null
      withClue("initial request must carry caller-supplied fields verbatim: ") {
        first.subscription shouldBe subscription
        first.streamAckDeadlineSeconds shouldBe 60
        first.clientId shouldBe "test-client"
        first.maxOutstandingMessages shouldBe 100L
        first.maxOutstandingBytes shouldBe 10485760L
      }

      val subsequent = Iterator
        .continually(Option(captured.poll()))
        .takeWhile(_.isDefined)
        .flatten
        .toList
      subsequent should not be empty
      subsequent.zipWithIndex.foreach { case (req, idx) =>
        withClue(s"subsequent request #${idx + 1} must be the default instance: ") {
          req.subscription shouldBe ""
          req.streamAckDeadlineSeconds shouldBe 0
          req.clientId shouldBe ""
          req.maxOutstandingMessages shouldBe 0L
          req.maxOutstandingBytes shouldBe 0L
        }
      }
    }
  }

  override def afterAll(): Unit =
    system.terminate()
}

/**
 * Stub that captures every StreamingPullRequest sent on the client → server stream
 * and keeps the response stream open indefinitely (so the polling tick keeps firing).
 */
class CapturingStreamingPullClient(captured: ConcurrentLinkedQueue[StreamingPullRequest])(
    implicit sys: ActorSystem) extends TestSubscriberClientBase {
  private implicit val mat: Materializer = Materializer.matFromSystem(sys)

  override def streamingPull(
      in: Source[StreamingPullRequest, NotUsed])
      : Source[StreamingPullResponse, NotUsed] =
    Source
      .maybe[StreamingPullResponse]
      .mapMaterializedValue { _ =>
        in.runForeach(req => captured.add(req))
        NotUsed
      }
}

/**
 * Base stub for [[SubscriberClient]] that provides default implementations
 * for all methods. Tests override only the methods they need.
 */
trait TestSubscriberClientBase extends SubscriberClient {
  override def createSubscription(in: Subscription): Future[Subscription] = ???
  override def getSubscription(in: GetSubscriptionRequest): Future[Subscription] = ???
  override def updateSubscription(in: UpdateSubscriptionRequest): Future[Subscription] = ???
  override def listSubscriptions(in: ListSubscriptionsRequest): Future[ListSubscriptionsResponse] = ???
  override def deleteSubscription(in: DeleteSubscriptionRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def modifyAckDeadline(in: ModifyAckDeadlineRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def acknowledge(in: AcknowledgeRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def pull(in: PullRequest): Future[PullResponse] = ???
  override def streamingPull(
      in: Source[StreamingPullRequest, NotUsed])
      : Source[StreamingPullResponse, NotUsed] = ???
  override def modifyPushConfig(in: ModifyPushConfigRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def getSnapshot(in: GetSnapshotRequest): Future[Snapshot] = ???
  override def listSnapshots(in: ListSnapshotsRequest): Future[ListSnapshotsResponse] = ???
  override def createSnapshot(in: CreateSnapshotRequest): Future[Snapshot] = ???
  override def updateSnapshot(in: UpdateSnapshotRequest): Future[Snapshot] = ???
  override def deleteSnapshot(in: DeleteSnapshotRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def seek(in: SeekRequest): Future[SeekResponse] = ???

  override def close(): Future[Done] = Future.successful(Done)
  override def closed: Future[Done] = Promise[Done]().future
}
