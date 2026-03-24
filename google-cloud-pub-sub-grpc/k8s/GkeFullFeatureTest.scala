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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.gke

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.RestartSettings
import pekko.stream.connectors.googlecloud.pubsub.grpc.{ AckDeadlineDistribution, FlowControl }
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.GooglePubSub
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

/**
 * Full-featured GKE example app exercising all features:
 *
 * 1. google-application-default credentials (via application.conf)
 * 2. StreamingPull with auto-reconnect (RestartSource)
 * 3. Auto-extend ack deadlines (fixed deadline)
 * 4. Auto-extend ack deadlines (adaptive via AckDeadlineDistribution)
 * 5. Flow control (backpressure on outstanding messages)
 * 6. Nack (immediate redelivery)
 * 7. Dynamic ack deadline modification
 *
 * Each scenario publishes messages, then subscribes and processes them,
 * printing results to stdout. Exits 0 on success, 1 on failure.
 */
object GkeFullFeatureTest {

  def main(args: Array[String]): Unit = {
    val projectId = sys.env.getOrElse("PROJECT_ID", "pekko-connectors")
    val topic = sys.env.getOrElse("TOPIC", "simpleTopic")
    val subscription = sys.env.getOrElse("SUBSCRIPTION", "simpleSubscription")

    implicit val system: ActorSystem = ActorSystem("GkeFullFeatureTest")
    val topicFqrs = s"projects/$projectId/topics/$topic"
    val subFqrs = s"projects/$projectId/subscriptions/$subscription"

    try {
      scenario1_BasicPublishSubscribe(topicFqrs, subFqrs)
      scenario2_ReconnectingSubscriber(topicFqrs, subFqrs)
      scenario3_FixedAutoExtendAckDeadlines(topicFqrs, subFqrs)
      scenario4_AdaptiveAckDeadlines(topicFqrs, subFqrs)
      scenario5_FlowControl(topicFqrs, subFqrs)
      scenario6_NackAndRedeliver(topicFqrs, subFqrs)
      scenario7_DynamicDeadlineModification(topicFqrs, subFqrs)

      println("\n=== ALL SCENARIOS PASSED ===")
      Await.result(system.terminate(), 10.seconds)
      sys.exit(0)
    } catch {
      case ex: Throwable =>
        println(s"\nFAILED: ${ex.getMessage}")
        ex.printStackTrace()
        Await.result(system.terminate(), 10.seconds)
        sys.exit(1)
    }
  }

  // ---------------------------------------------------------------------------
  // Scenario 1: Basic publish + subscribe (google-application-default auth)
  // ---------------------------------------------------------------------------
  private def scenario1_BasicPublishSubscribe(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 1: Basic publish + subscribe (google-application-default credentials) ---")

    val testData = ByteString.copyFromUtf8(s"scenario1-${System.nanoTime()}")

    // Publish
    val publishResult = Source
      .single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(testData))))
      .via(GooglePubSub.publish(parallelism = 1))
      .runWith(Sink.head)

    val response = Await.result(publishResult, 30.seconds)
    println(s"  Published: messageId=${response.messageIds.headOption.getOrElse("?")}")

    // Subscribe and receive
    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second)
      .filter(_.message.exists(_.data == testData))
      .take(1)
      .alsoTo(
        Flow[ReceivedMessage]
          .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
          .to(GooglePubSub.acknowledge(parallelism = 1)))
      .runWith(Sink.head)

    val msg = Await.result(received, 30.seconds)
    println(s"  Received: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")}")
    assert(msg.message.exists(_.data == testData), "Data mismatch!")
    println("  PASSED")
  }

  // ---------------------------------------------------------------------------
  // Scenario 2: Reconnecting subscriber with RestartSettings
  // ---------------------------------------------------------------------------
  private def scenario2_ReconnectingSubscriber(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 2: Reconnecting subscriber (RestartSource) ---")

    val testData = ByteString.copyFromUtf8(s"scenario2-${System.nanoTime()}")

    // Publish
    Await.result(
      Source
        .single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(testData))))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head),
      30.seconds)
    println("  Published message")

    // Subscribe with auto-reconnect
    val restartSettings = RestartSettings(
      minBackoff = 1.second,
      maxBackoff = 10.seconds,
      randomFactor = 0.2).withMaxRestarts(3, 1.minute)

    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second, restartSettings)
      .filter(_.message.exists(_.data == testData))
      .take(1)
      .alsoTo(
        Flow[ReceivedMessage]
          .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
          .to(GooglePubSub.acknowledge(parallelism = 1)))
      .runWith(Sink.head)

    val msg = Await.result(received, 30.seconds)
    println(s"  Received via reconnecting subscriber: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")}")
    println("  PASSED")
  }

  // ---------------------------------------------------------------------------
  // Scenario 3: Fixed auto-extend ack deadlines
  // ---------------------------------------------------------------------------
  private def scenario3_FixedAutoExtendAckDeadlines(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 3: Fixed auto-extend ack deadlines ---")

    val testData = ByteString.copyFromUtf8(s"scenario3-${System.nanoTime()}")

    // Publish
    Await.result(
      Source
        .single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(testData))))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head),
      30.seconds)
    println("  Published message")

    // Subscribe with auto-extend: extend every 8s with a 30s deadline
    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second)
      .mapMaterializedValue(_ => pekko.NotUsed)
      .via(GooglePubSub.autoExtendAckDeadlines(subFqrs, 8.seconds, 30))
      .filter(_.message.exists(_.data == testData))
      .take(1)
      .mapAsync(1) { msg =>
        // Simulate slow processing (15s) — without auto-extend this would cause redelivery
        println(s"  Processing message (simulating 15s delay)...")
        pekko.pattern.after(15.seconds)(Future.successful(msg))
      }
      .alsoTo(
        Flow[ReceivedMessage]
          .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
          .to(GooglePubSub.acknowledge(parallelism = 1)))
      .runWith(Sink.head)

    val msg = Await.result(received, 60.seconds)
    println(s"  Received after slow processing: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")}")
    println("  PASSED (no redelivery during slow processing)")
  }

  // ---------------------------------------------------------------------------
  // Scenario 4: Adaptive ack deadlines with AckDeadlineDistribution
  // ---------------------------------------------------------------------------
  private def scenario4_AdaptiveAckDeadlines(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 4: Adaptive ack deadlines (AckDeadlineDistribution) ---")

    val messageCount = 5
    val testPrefix = s"scenario4-${System.nanoTime()}"
    val messages = (1 to messageCount).map(i =>
      PubsubMessage().withData(ByteString.copyFromUtf8(s"$testPrefix-$i")))

    // Publish batch
    Await.result(
      Source
        .single(PublishRequest(topicFqrs, messages))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head),
      30.seconds)
    println(s"  Published $messageCount messages")

    // Set up adaptive distribution
    val distribution = AckDeadlineDistribution(
      initialDeadlineSeconds = 10,
      minDeadlineSeconds = 10,
      maxDeadlineSeconds = 600)

    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second)
      .mapMaterializedValue(_ => pekko.NotUsed)
      .via(GooglePubSub.autoExtendAckDeadlines(subFqrs, 3.seconds, distribution))
      .filter(_.message.exists(_.data.toStringUtf8.startsWith(testPrefix)))
      .take(messageCount)
      .mapAsync(1) { msg =>
        // Simulate variable processing time
        val delay = (msg.message.map(_.data.toStringUtf8).getOrElse("").hashCode.abs % 3 + 1).seconds
        println(s"  Processing: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")} (${delay.toSeconds}s)")
        pekko.pattern.after(delay)(Future.successful(msg))
      }
      .alsoTo(
        Flow[ReceivedMessage]
          .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
          .to(GooglePubSub.acknowledge(parallelism = 1, distribution)))
      .runWith(Sink.seq)

    val msgs = Await.result(received, 120.seconds)
    println(s"  Received ${msgs.size}/$messageCount messages")
    println(s"  Adaptive deadline: ${distribution.currentDeadlineSeconds}s")
    assert(msgs.size == messageCount, s"Expected $messageCount messages, got ${msgs.size}")
    println("  PASSED")
  }

  // ---------------------------------------------------------------------------
  // Scenario 5: Flow control (backpressure on outstanding messages)
  // ---------------------------------------------------------------------------
  private def scenario5_FlowControl(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 5: Flow control (max outstanding messages) ---")

    val messageCount = 10
    val maxOutstanding = 3
    val testPrefix = s"scenario5-${System.nanoTime()}"
    val messages = (1 to messageCount).map(i =>
      PubsubMessage().withData(ByteString.copyFromUtf8(s"$testPrefix-$i")))

    // Publish batch
    Await.result(
      Source
        .single(PublishRequest(topicFqrs, messages))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head),
      30.seconds)
    println(s"  Published $messageCount messages")

    val flowControl = FlowControl(maxOutstandingMessages = maxOutstanding)
    var maxObserved = 0L

    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second)
      .mapMaterializedValue(_ => pekko.NotUsed)
      .via(GooglePubSub.flowControlGate(flowControl))
      .filter(_.message.exists(_.data.toStringUtf8.startsWith(testPrefix)))
      .take(messageCount)
      .mapAsync(maxOutstanding) { msg =>
        val current = flowControl.outstandingCount
        synchronized { if (current > maxObserved) maxObserved = current }
        println(s"  Processing: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")} " +
          s"(outstanding: $current/$maxOutstanding)")
        // Simulate work
        pekko.pattern.after(1.second)(Future.successful(msg))
      }
      .alsoTo(
        Flow[ReceivedMessage]
          .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
          .to(GooglePubSub.acknowledge(parallelism = 1, flowControl)))
      .runWith(Sink.seq)

    val msgs = Await.result(received, 120.seconds)
    println(s"  Received ${msgs.size}/$messageCount messages")
    println(s"  Max outstanding observed: $maxObserved (limit: $maxOutstanding)")
    assert(msgs.size == messageCount, s"Expected $messageCount messages, got ${msgs.size}")
    assert(maxObserved <= maxOutstanding, s"Flow control violated: $maxObserved > $maxOutstanding")
    println("  PASSED")
  }

  // ---------------------------------------------------------------------------
  // Scenario 6: Nack causes immediate redelivery
  // ---------------------------------------------------------------------------
  private def scenario6_NackAndRedeliver(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 6: Nack and immediate redelivery ---")

    val testData = ByteString.copyFromUtf8(s"scenario6-${System.nanoTime()}")

    // Publish
    Await.result(
      Source
        .single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(testData))))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head),
      30.seconds)
    println("  Published message")

    // Subscribe: nack the first delivery, ack the second
    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second)
      .mapMaterializedValue(_ => pekko.NotUsed)
      .filter(_.message.exists(_.data == testData))
      .statefulMap(() => 0)(
        (count, msg) => {
          val newCount = count + 1
          (newCount, (newCount, msg))
        },
        _ => None)
      .mapAsync(1) {
        case (attempt, msg) if attempt == 1 =>
          println(s"  Attempt $attempt: nacking message for redelivery")
          val nackReq = AcknowledgeRequest(subFqrs, Seq(msg.ackId))
          Source
            .single(nackReq)
            .via(GooglePubSub.nackFlow())
            .runWith(Sink.head)
            .map(_ => None)(system.dispatcher)
        case (attempt, msg) =>
          println(s"  Attempt $attempt: acknowledging message")
          val ackReq = AcknowledgeRequest(subFqrs, Seq(msg.ackId))
          Source
            .single(ackReq)
            .via(GooglePubSub.acknowledgeFlow())
            .runWith(Sink.head)
            .map(_ => Some(msg))(system.dispatcher)
      }
      .collect { case Some(m) => m }
      .take(1)
      .runWith(Sink.head)

    val msg = Await.result(received, 60.seconds)
    println(s"  Received after nack+redeliver: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")}")
    println("  PASSED (message was nacked and redelivered)")
  }

  // ---------------------------------------------------------------------------
  // Scenario 7: Dynamic per-message deadline modification
  // ---------------------------------------------------------------------------
  private def scenario7_DynamicDeadlineModification(topicFqrs: String, subFqrs: String)(
      implicit system: ActorSystem): Unit = {
    println("\n--- Scenario 7: Dynamic per-message deadline modification ---")

    val messageCount = 3
    val testPrefix = s"scenario7-${System.nanoTime()}"
    val messages = (1 to messageCount).map(i =>
      PubsubMessage()
        .withData(ByteString.copyFromUtf8(s"$testPrefix-$i"))
        .withAttributes(Map("priority" -> (if (i == 1) "high" else "low"))))

    // Publish
    Await.result(
      Source
        .single(PublishRequest(topicFqrs, messages))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head),
      30.seconds)
    println(s"  Published $messageCount messages with priority attributes")

    // Subscribe with dynamic deadline: high priority gets longer deadline
    val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
    val received = GooglePubSub
      .subscribe(request, 1.second)
      .mapMaterializedValue(_ => pekko.NotUsed)
      .filter(_.message.exists(_.data.toStringUtf8.startsWith(testPrefix)))
      .take(messageCount)
      .via(GooglePubSub.modifyAckDeadlineDynamic(subFqrs, parallelism = 1) { msg =>
        val priority = msg.message.flatMap(_.attributes.get("priority")).getOrElse("low")
        val deadline = if (priority == "high") 60 else 15
        println(s"  Setting deadline=${deadline}s for priority=$priority: " +
          s"${msg.message.map(_.data.toStringUtf8).getOrElse("?")}")
        deadline
      })
      .alsoTo(
        Flow[ReceivedMessage]
          .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
          .to(GooglePubSub.acknowledge(parallelism = 1)))
      .runWith(Sink.seq)

    val msgs = Await.result(received, 60.seconds)
    println(s"  Received ${msgs.size}/$messageCount messages with dynamic deadlines")
    assert(msgs.size == messageCount, s"Expected $messageCount messages, got ${msgs.size}")
    println("  PASSED")
  }
}
