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
import pekko.Done
import pekko.actor.ActorSystem
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.{ GooglePubSub, GrpcSubscriber, PubSubAttributes }
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AutoExtendAckDeadlinesSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

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

  override def afterAll(): Unit =
    system.terminate()
}

/**
 * Base stub for [[SubscriberClient]] that provides default implementations
 * for all methods. Tests override only the methods they need.
 */
trait TestSubscriberClientBase extends SubscriberClient {
  import pekko.NotUsed

  override def createSubscription(in: Subscription): Future[Subscription] = ???
  override def getSubscription(in: GetSubscriptionRequest): Future[Subscription] = ???
  override def updateSubscription(in: UpdateSubscriptionRequest): Future[Subscription] = ???
  override def listSubscriptions(in: ListSubscriptionsRequest): Future[ListSubscriptionsResponse] = ???
  override def deleteSubscription(in: DeleteSubscriptionRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def modifyAckDeadline(in: ModifyAckDeadlineRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def acknowledge(in: AcknowledgeRequest): Future[com.google.protobuf.empty.Empty] = ???
  override def pull(in: PullRequest): Future[PullResponse] = ???
  override def streamingPull(
      in: pekko.stream.scaladsl.Source[StreamingPullRequest, NotUsed])
      : pekko.stream.scaladsl.Source[StreamingPullResponse, NotUsed] = ???
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
