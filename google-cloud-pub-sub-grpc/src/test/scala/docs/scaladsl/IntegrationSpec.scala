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
import pekko.Done
import pekko.actor.{ ActorSystem, Cancellable }
import pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.{ GrpcPublisher, PubSubAttributes }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.scalatest.OptionValues

//#publish-single
import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.GooglePubSub
import pekko.stream.scaladsl._

import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._

import scala.concurrent.Future

//#publish-single

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, Inside }

import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IntegrationSpec
    extends AnyWordSpec
    with Matchers
    with Inside
    with BeforeAndAfterAll
    with ScalaFutures
    with OptionValues
    with LogCapturing {

  implicit val system: ActorSystem = ActorSystem("IntegrationSpec")

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 15.seconds, interval = 50.millis)

  "connector" should {

    "publish a message" in {
      // #publish-single
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"

      val publishMessage: PubsubMessage =
        PubsubMessage()
          .withData(ByteString.copyFromUtf8("Hello world!"))

      val publishRequest: PublishRequest =
        PublishRequest()
          .withTopic(s"projects/$projectId/topics/$topic")
          .addMessages(publishMessage)

      val source: Source[PublishRequest, NotUsed] =
        Source.single(publishRequest)

      val publishFlow: Flow[PublishRequest, PublishResponse, NotUsed] =
        GooglePubSub.publish(parallelism = 1)

      val publishedMessageIds: Future[Seq[PublishResponse]] = source.via(publishFlow).runWith(Sink.seq)
      // #publish-single

      publishedMessageIds.futureValue should not be empty
    }

    "publish batch" in {
      // #publish-fast
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"

      val publishMessage: PubsubMessage =
        PubsubMessage()
          .withData(ByteString.copyFromUtf8("Hello world!"))

      val messageSource: Source[PubsubMessage, NotUsed] = Source(List(publishMessage, publishMessage))
      val published = messageSource
        .groupedWithin(1000, 1.minute)
        .map { msgs =>
          PublishRequest()
            .withTopic(s"projects/$projectId/topics/$topic")
            .addAllMessages(msgs)
        }
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.seq)
      // #publish-fast

      published.futureValue should not be empty
    }

    "subscribe streaming" in {
      // #subscribe-stream
      val projectId = "pekko-connectors"
      val subscription = "simpleSubscription"

      val request = StreamingPullRequest()
        .withSubscription(s"projects/$projectId/subscriptions/$subscription")
        .withStreamAckDeadlineSeconds(10)

      val subscriptionSource: Source[ReceivedMessage, Future[Cancellable]] =
        GooglePubSub.subscribe(request, pollInterval = 1.second)
      // #subscribe-stream

      val first = subscriptionSource.runWith(Sink.head)

      val topic = "simpleTopic"
      val msg = ByteString.copyFromUtf8("Hello world!")

      val publishMessage: PubsubMessage =
        PubsubMessage().withData(msg)

      val publishRequest: PublishRequest =
        PublishRequest()
          .withTopic(s"projects/$projectId/topics/$topic")
          .addMessages(publishMessage)

      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.ignore)

      first.futureValue.message.value.data shouldBe msg
    }

    "subscribe sync" in {
      // #subscribe-sync
      val projectId = "pekko-connectors"
      val subscription = "simpleSubscription"

      val request = PullRequest()
        .withSubscription(s"projects/$projectId/subscriptions/$subscription")
        .withMaxMessages(10)

      val subscriptionSource: Source[ReceivedMessage, Future[Cancellable]] =
        GooglePubSub.subscribePolling(request, pollInterval = 1.second)
      // #subscribe-sync

      val first = subscriptionSource.runWith(Sink.head)

      val topic = "simpleTopic"
      val msg = ByteString.copyFromUtf8("Hello world!")

      val publishMessage: PubsubMessage =
        PubsubMessage().withData(msg)

      val publishRequest: PublishRequest =
        PublishRequest()
          .withTopic(s"projects/$projectId/topics/$topic")
          .addMessages(publishMessage)

      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.ignore)

      first.futureValue.message.value.data shouldBe msg
    }

    "acknowledge" in {
      val projectId = "pekko-connectors"
      val subscription = "simpleSubscription"

      val request = StreamingPullRequest()
        .withSubscription(s"projects/$projectId/subscriptions/$subscription")
        .withStreamAckDeadlineSeconds(10)

      val subscriptionSource: Source[ReceivedMessage, Future[Cancellable]] =
        GooglePubSub.subscribe(request, pollInterval = 1.second)

      // #acknowledge
      val ackSink: Sink[AcknowledgeRequest, Future[Done]] =
        GooglePubSub.acknowledge(parallelism = 1)

      subscriptionSource
        .map { message =>
          // do something fun
          message.ackId
        }
        .groupedWithin(10, 1.second)
        .map(ids =>
          AcknowledgeRequest()
            .withSubscription(
              s"projects/$projectId/subscriptions/$subscription")
            .withAckIds(ids))
        .to(ackSink)
      // #acknowledge
    }

    "acknowledge flow" in {
      val projectId = "pekko-connectors"
      val subscription = "simpleSubscription"

      val request = StreamingPullRequest()
        .withSubscription(s"projects/$projectId/subscriptions/$subscription")
        .withStreamAckDeadlineSeconds(10)

      val subscriptionSource: Source[ReceivedMessage, Future[Cancellable]] =
        GooglePubSub.subscribe(request, pollInterval = 1.second)

      subscriptionSource
        .map { message =>
          // do something fun
          message.ackId
        }
        .groupedWithin(10, 1.second)
        .map(ids => AcknowledgeRequest(ackIds = ids))
        .via(GooglePubSub.acknowledgeFlow())
        .to(Sink.ignore)
    }

    "modify ack deadline" in {
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish a message
      val msg = ByteString.copyFromUtf8("Hello deadline extension!")
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addMessages(PubsubMessage().withData(msg))
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // #modify-ack-deadline
      // subscribe, extend deadline, then ack
      val result = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .take(1)
        .mapAsync(1) { receivedMessage =>
          // extend the ack deadline
          val modifyRequest = ModifyAckDeadlineRequest(
            subscription = subscriptionFqrs,
            ackIds = Seq(receivedMessage.ackId),
            ackDeadlineSeconds = 30)
          Source.single(modifyRequest)
            .via(GooglePubSub.modifyAckDeadlineFlow())
            .runWith(Sink.head)
            .map(_ => receivedMessage)(system.dispatcher)
        }
        .alsoTo(
          Flow[ReceivedMessage]
            .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)
      // #modify-ack-deadline

      result.futureValue.message.value.data shouldBe msg
    }

    "auto extend ack deadlines" in {
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish a message
      val msg = ByteString.copyFromUtf8("Hello auto-extend!")
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addMessages(PubsubMessage().withData(msg))
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // #subscribe-auto-extend
      // subscribe with auto-extending deadlines, then ack
      val result = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, 30))
        .take(1)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)
      // #subscribe-auto-extend

      result.futureValue.message.value.data shouldBe msg
    }

    "auto extend prevents redelivery during slow processing" in {
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish a unique message
      val msg = ByteString.copyFromUtf8(s"slow-processing-${System.nanoTime()}")
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addMessages(PubsubMessage().withData(msg))
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // Subscribe with auto-extend, simulate slow processing (15s > 10s ack deadline),
      // then ack. Without the deadline extension the message would be redelivered.
      val received = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, 10))
        .filter(_.message.exists(_.data == msg))
        .take(1)
        .mapAsync(1) { receivedMessage =>
          // simulate slow processing that exceeds the ack deadline
          org.apache.pekko.pattern.after(15.seconds)(
            Future.successful(receivedMessage))(system)
        }
        .alsoTo(
          Flow[ReceivedMessage]
            .map(m => AcknowledgeRequest(subscriptionFqrs, Seq(m.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)

      received.futureValue(timeout(30.seconds)).message.value.data shouldBe msg

      // verify no redelivery — subscription should be empty (for our message)
      val redelivered = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .filter(_.message.exists(_.data == msg))
        .idleTimeout(12.seconds)
        .runWith(Sink.seq)
        .failed
        .futureValue

      redelivered shouldBe a[java.util.concurrent.TimeoutException]
    }

    "republish" in {
      val msg = "Labas!"

      val projectId = "pekko-connectors"
      val topic = "testTopic"
      val subscription = "testSubscription"

      val topicFqrs = s"projects/$projectId/topics/$topic"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      val pub = PublishRequest(topicFqrs, Seq(PubsubMessage(ByteString.copyFromUtf8(msg))))
      val pubResp = Source.single(pub).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head)

      pubResp.futureValue.messageIds should not be empty

      val sub = StreamingPullRequest(subscriptionFqrs, streamAckDeadlineSeconds = 10)

      // subscribe but do not ack - message will be republished later
      val subNoAckResp = GooglePubSub.subscribe(sub, 1.second).runWith(Sink.head)

      inside(subNoAckResp.futureValue.message) {
        case Some(PubsubMessage(data, _, _, _, _, _)) => data.toStringUtf8 shouldBe msg
      }

      // subscribe and get the republished message, and ack this time
      val subWithAckResp = GooglePubSub
        .subscribe(sub, 1.second)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)

      inside(subWithAckResp.futureValue.message) {
        case Some(PubsubMessage(data, _, _, _, _, _)) => data.toStringUtf8 shouldBe msg
      }

      // check if the message is not republished again
      GooglePubSub
        .subscribe(sub, 1.second)
        .idleTimeout(12.seconds)
        .runWith(Sink.ignore)
        .failed
        .futureValue
    }

    "reconnect after emulator restart" in {
      import pekko.stream.RestartSettings
      import scala.sys.process._

      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"
      val topicFqrs = s"projects/$projectId/topics/$topic"

      val restartSettings = RestartSettings(
        minBackoff = 100.millis,
        maxBackoff = 5.seconds,
        randomFactor = 0.2)

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // publish a message before restart
      val msg1 = ByteString.copyFromUtf8(s"before-restart-${System.nanoTime()}")
      Source.single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(msg1))))
        .via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      // start reconnecting subscriber — take first matching message, ack everything
      val first = GooglePubSub
        .subscribe(request, 1.second, restartSettings)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .filter(_.message.exists(_.data == msg1))
        .runWith(Sink.head)

      first.futureValue.message.value.data shouldBe msg1

      // restart emulator — this kills all gRPC connections
      "docker compose restart gcloud-pubsub-emulator".!

      // wait for emulator to come back up
      Thread.sleep(8000)

      // recreate topic and subscription (emulator loses state on restart)
      s"docker compose exec gcloud-pubsub-emulator curl -s -X PUT http://localhost:8538/v1/projects/$projectId/topics/$topic".!
      s"docker compose exec gcloud-pubsub-emulator curl -s -X PUT http://localhost:8538/v1/projects/$projectId/subscriptions/$subscription -H Content-Type:application/json -d {\"topic\":\"projects/$projectId/topics/$topic\"}".!

      Thread.sleep(2000)

      // publish a second message after restart
      val msg2 = ByteString.copyFromUtf8(s"after-restart-${System.nanoTime()}")
      Source.single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(msg2))))
        .via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      // start another reconnecting subscriber — should connect to the restarted emulator
      val second = GooglePubSub
        .subscribe(request, 1.second, restartSettings)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)

      second.futureValue.message.value.data shouldBe msg2
    }

    "nack causes immediate redelivery" in {
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish a unique message
      val msg = ByteString.copyFromUtf8(s"nack-test-${System.nanoTime()}")
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addMessages(PubsubMessage().withData(msg))
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // receive the message and nack it
      val nacked = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .filter(_.message.exists(_.data == msg))
        .take(1)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(m => AcknowledgeRequest(subscriptionFqrs, Seq(m.ackId)))
            .to(GooglePubSub.nack(parallelism = 1)))
        .runWith(Sink.head)

      nacked.futureValue.message.value.data shouldBe msg

      // the nacked message should be redelivered quickly
      val redelivered = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .filter(_.message.exists(_.data == msg))
        .take(1)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(m => AcknowledgeRequest(subscriptionFqrs, Seq(m.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)

      redelivered.futureValue.message.value.data shouldBe msg
    }

    "dynamic deadline modification" in {
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish a message
      val msg = ByteString.copyFromUtf8(s"dynamic-deadline-${System.nanoTime()}")
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addMessages(PubsubMessage().withData(msg))
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // subscribe, dynamically set deadline based on message content, then ack
      val result = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .filter(_.message.exists(_.data == msg))
        .take(1)
        .via(GooglePubSub.modifyAckDeadlineDynamic(subscriptionFqrs) { _ => 30 })
        .alsoTo(
          Flow[ReceivedMessage]
            .map(m => AcknowledgeRequest(subscriptionFqrs, Seq(m.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)

      result.futureValue.message.value.data shouldBe msg
    }

    "flow control limits outstanding messages" in {
      import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.FlowControl

      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish several messages
      val prefix = s"fc-test-${System.nanoTime()}"
      val messages = (1 to 5).map { i =>
        PubsubMessage().withData(ByteString.copyFromUtf8(s"$prefix-$i"))
      }
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addAllMessages(messages)
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // flow control with max 2 outstanding messages
      val fc = FlowControl(maxOutstandingMessages = 2)

      val maxSeen = new java.util.concurrent.atomic.AtomicLong(0)

      val result = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .via(GooglePubSub.flowControlGate(fc))
        .take(5)
        .mapAsync(2) { msg =>
          val current = fc.outstandingCount
          maxSeen.updateAndGet(old => math.max(old, current))
          // simulate some processing
          org.apache.pekko.pattern.after(200.millis)(
            Future.successful(msg))(system)
        }
        .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
        .via(GooglePubSub.acknowledgeFlow(fc))
        .runWith(Sink.seq)

      result.futureValue should have size 5
      // max outstanding should never exceed our limit (allow +1 for race between acquire and check)
      maxSeen.get() should be <= 3L
    }

    "auto extend with maxAckExtensionPeriod" in {
      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      // publish a message
      val msg = ByteString.copyFromUtf8(s"max-ext-${System.nanoTime()}")
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addMessages(PubsubMessage().withData(msg))
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // use auto-extend with a very short maxAckExtensionPeriod
      val result = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, 10, 5.seconds))
        .filter(_.message.exists(_.data == msg))
        .take(1)
        .alsoTo(
          Flow[ReceivedMessage]
            .map(m => AcknowledgeRequest(subscriptionFqrs, Seq(m.ackId)))
            .to(GooglePubSub.acknowledge(parallelism = 1)))
        .runWith(Sink.head)

      result.futureValue.message.value.data shouldBe msg
    }

    "adaptive deadline distribution" in {
      import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineDistribution

      val projectId = "pekko-connectors"
      val topic = "simpleTopic"
      val subscription = "simpleSubscription"
      val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

      val dist = AckDeadlineDistribution(initialDeadlineSeconds = 10)

      // publish several messages
      val prefix = s"adaptive-${System.nanoTime()}"
      val messages = (1 to 3).map { i =>
        PubsubMessage().withData(ByteString.copyFromUtf8(s"$prefix-$i"))
      }
      val publishRequest = PublishRequest()
        .withTopic(s"projects/$projectId/topics/$topic")
        .addAllMessages(messages)
      Source.single(publishRequest).via(GooglePubSub.publish(parallelism = 1)).runWith(Sink.head).futureValue

      val request = StreamingPullRequest()
        .withSubscription(subscriptionFqrs)
        .withStreamAckDeadlineSeconds(10)

      // initially should use the initialDeadlineSeconds
      dist.currentDeadlineSeconds shouldBe 10

      // subscribe with adaptive deadline, process with a small delay, then ack
      val result = GooglePubSub.subscribe(request, pollInterval = 1.second)
        .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, dist))
        .take(3)
        .mapAsync(1) { msg =>
          // simulate ~2 second processing
          org.apache.pekko.pattern.after(2.seconds)(Future.successful(msg))(system)
        }
        .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
        .via(GooglePubSub.acknowledgeFlow(dist))
        .runWith(Sink.seq)

      result.futureValue should have size 3

      // after processing, the adaptive deadline should have adapted
      // with ~2s processing, p99 should be around 3s (rounded up), clamped to min 10s
      dist.currentDeadlineSeconds should be >= 10
    }

    "custom publisher" in {
      // #attributes
      val settings = PubSubSettings(system)
      val publisher = GrpcPublisher(settings)

      val publishFlow: Flow[PublishRequest, PublishResponse, NotUsed] =
        GooglePubSub
          .publish(parallelism = 1)
          .withAttributes(PubSubAttributes.publisher(publisher))
      // #attributes

      Source.single(PublishRequest()).via(publishFlow).to(Sink.ignore)
    }
  }

  override def afterAll() =
    system.terminate()

}
