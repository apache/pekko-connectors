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
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.GooglePubSub
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Minimal app to verify Pub/Sub auth on GKE.
 * Publishes a message then subscribes to confirm it arrived.
 * Exits 0 on success, 1 on failure.
 */
object GkeAuthTest {
  def main(args: Array[String]): Unit = {
    val projectId = sys.env.getOrElse("PROJECT_ID", "pekko-connectors")
    val topic = sys.env.getOrElse("TOPIC", "simpleTopic")
    val subscription = sys.env.getOrElse("SUBSCRIPTION", "simpleSubscription")

    implicit val system: ActorSystem = ActorSystem("GkeAuthTest")
    import system.dispatcher

    try {
      val topicFqrs = s"projects/$projectId/topics/$topic"
      val subFqrs = s"projects/$projectId/subscriptions/$subscription"
      val testData = ByteString.copyFromUtf8(s"gke-auth-test-${System.nanoTime()}")

      // Publish
      println(s"Publishing to $topicFqrs ...")
      val publishResult = Source
        .single(PublishRequest(topicFqrs, Seq(PubsubMessage().withData(testData))))
        .via(GooglePubSub.publish(parallelism = 1))
        .runWith(Sink.head)

      val response = Await.result(publishResult, 30.seconds)
      println(s"Published: messageId=${response.messageIds.headOption.getOrElse("?")}")

      // Subscribe
      println(s"Subscribing to $subFqrs ...")
      val request = StreamingPullRequest(subFqrs, streamAckDeadlineSeconds = 10)
      val received = Source.futureSource(
        GooglePubSub.subscribe(request, 1.second)
          .filter(_.message.exists(_.data == testData))
          .take(1)
          .alsoTo(
            Flow[ReceivedMessage]
              .map(m => AcknowledgeRequest(subFqrs, Seq(m.ackId)))
              .to(GooglePubSub.acknowledge(parallelism = 1)))
          .runWith(Sink.head)
          .map(Source.single)(dispatcher))
        .runWith(Sink.head)

      val msg = Await.result(received, 30.seconds)
      println(s"Received: ${msg.message.map(_.data.toStringUtf8).getOrElse("?")}")
      println("SUCCESS: GKE auth test passed")
      Await.result(system.terminate(), 10.seconds)
      sys.exit(0)
    } catch {
      case ex: Throwable =>
        println(s"FAILED: ${ex.getMessage}")
        ex.printStackTrace()
        Await.result(system.terminate(), 10.seconds)
        sys.exit(1)
    }
  }
}
