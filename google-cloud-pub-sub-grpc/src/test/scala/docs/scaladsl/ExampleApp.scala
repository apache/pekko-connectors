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
import java.util.logging.{ Level, Logger }
import org.apache.pekko
import pekko.actor.{ ActorSystem, Cancellable }
import pekko.stream.{ DelayOverflowStrategy, RestartSettings }
import pekko.stream.connectors.googlecloud.pubsub.grpc.FlowControl
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.GooglePubSub
import pekko.stream.scaladsl.{ Sink, Source }
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub.{
  AcknowledgeRequest,
  PublishRequest,
  PubsubMessage,
  ReceivedMessage,
  StreamingPullRequest
}
import com.typesafe.config.ConfigFactory

import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object ExampleApp {

  Logger.getLogger("io.grpc.netty").setLevel(Level.OFF)

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.parseString("""
        |pekko.loglevel = INFO
      """.stripMargin)

    implicit val sys: ActorSystem = ActorSystem("ExampleApp", config)

    import sys.dispatcher

    val result = args.toList match {
      case "publish-single" :: rest             => publishSingle(rest)
      case "publish-stream" :: rest             => publishStream(rest)
      case "subscribe" :: rest                  => subscribeStream(rest)
      case "subscribe-auto-extend" :: rest      => subscribeAutoExtend(rest)
      case "subscribe-flow-control" :: rest     => subscribeFlowControl(rest)
      case "subscribe-nack-retry" :: rest       => subscribeNackRetry(rest)
      case "subscribe-dynamic-deadline" :: rest => subscribeDynamicDeadline(rest)
      case "subscribe-adaptive" :: rest         => subscribeAdaptive(rest)
      case other                                => Future.failed(new Error(s"unknown arguments: $other"))
    }

    result.onComplete { res =>
      res match {
        case Success(c: Cancellable) =>
          println("### Press ENTER to stop the application.")
          scala.io.StdIn.readLine()
          c.cancel()
        case Success(s) =>
          println(s)
        case Failure(ex) =>
          ex.printStackTrace()
      }
      sys.terminate()
    }
  }

  private def publishSingle(args: List[String])(implicit system: ActorSystem) = {
    val projectId :: topic :: Nil = args: @nowarn("msg=match may not be exhaustive")

    Source
      .single(publish(projectId, topic)("Hello!"))
      .via(GooglePubSub.publish(parallelism = 1))
      .runWith(Sink.head)
  }

  private def publishStream(args: List[String])(implicit system: ActorSystem) = {
    val projectId :: topic :: Nil = args: @nowarn("msg=match may not be exhaustive")

    Source
      .tick(0.seconds, 1.second, ())
      .map(_ => {
        val temp = math.random() * 10 + 15
        f"Current temperature is: $temp%2.2f"
      })
      .delay(1.second, DelayOverflowStrategy.backpressure)
      .map(publish(projectId, topic)(_))
      .via(GooglePubSub.publish(parallelism = 1))
      .to(Sink.ignore)
      .mapMaterializedValue(Future.successful)
      .run()
  }

  private def subscribeStream(args: List[String])(implicit system: ActorSystem) = {
    val projectId :: sub :: Nil = args: @nowarn("msg=match may not be exhaustive")

    GooglePubSub
      .subscribe(subscribe(projectId, sub), 1.second)
      .to(Sink.foreach(println))
      .run()
  }

  private def subscribeAutoExtend(args: List[String])(implicit system: ActorSystem) = {
    val projectId :: sub :: Nil = args: @nowarn("msg=match may not be exhaustive")
    val subscriptionFqrs = subFqrs(projectId, sub)

    val restartSettings = RestartSettings(
      minBackoff = 100.millis,
      maxBackoff = 10.seconds,
      randomFactor = 0.2)

    GooglePubSub
      .subscribe(subscribe(projectId, sub), 1.second, restartSettings)
      .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, 30))
      .map { msg =>
        println(msg)
        AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId))
      }
      .to(GooglePubSub.acknowledge(parallelism = 1))
      .mapMaterializedValue(Future.successful(_))
      .run()
  }

  /**
   * Subscribe with flow control — limits the number of in-flight messages to prevent
   * memory exhaustion during slow processing.
   *
   * Usage: subscribe-flow-control <projectId> <subscription>
   */
  private def subscribeFlowControl(args: List[String])(implicit system: ActorSystem) = {
    val projectId :: sub :: Nil = args: @nowarn("msg=match may not be exhaustive")
    val subscriptionFqrs = subFqrs(projectId, sub)

    val restartSettings = RestartSettings(
      minBackoff = 100.millis,
      maxBackoff = 10.seconds,
      randomFactor = 0.2)

    val fc = FlowControl(maxOutstandingMessages = 100)

    GooglePubSub
      .subscribe(subscribe(projectId, sub), 1.second, restartSettings)
      .via(GooglePubSub.flowControlGate(fc))
      .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 8.seconds, 30))
      .map { msg =>
        println(s"[outstanding=${fc.outstandingCount}] ${msg.message.map(_.data.toStringUtf8).getOrElse("")}")
        AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId))
      }
      .to(GooglePubSub.acknowledge(parallelism = 1, fc))
      .mapMaterializedValue(Future.successful(_))
      .run()
  }

  /**
   * Subscribe and nack messages that fail processing, causing immediate redelivery.
   * Messages that succeed are acknowledged normally.
   *
   * Usage: subscribe-nack-retry <projectId> <subscription>
   */
  private def subscribeNackRetry(args: List[String])(implicit system: ActorSystem) = {
    import system.dispatcher

    val projectId :: sub :: Nil = args: @nowarn("msg=match may not be exhaustive")
    val subscriptionFqrs = subFqrs(projectId, sub)

    val restartSettings = RestartSettings(
      minBackoff = 100.millis,
      maxBackoff = 10.seconds,
      randomFactor = 0.2)

    GooglePubSub
      .subscribe(subscribe(projectId, sub), 1.second, restartSettings)
      .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 8.seconds, 30))
      .mapAsync(4) { msg =>
        processMessage(msg).map(Right(_)).recover { case ex =>
          println(s"Processing failed: ${ex.getMessage}, nacking for redelivery")
          Left(msg)
        }
      }
      .to(Sink.foreach {
        case Right(msg) =>
          Source.single(AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .runWith(GooglePubSub.acknowledge(parallelism = 1))
        case Left(msg) =>
          Source.single(AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
            .runWith(GooglePubSub.nack(parallelism = 1))
      })
      .mapMaterializedValue(Future.successful(_))
      .run()
  }

  /**
   * Subscribe with per-message dynamic deadline based on message size.
   * Larger messages get longer deadlines.
   *
   * Usage: subscribe-dynamic-deadline <projectId> <subscription>
   */
  private def subscribeDynamicDeadline(args: List[String])(implicit system: ActorSystem) = {
    val projectId :: sub :: Nil = args: @nowarn("msg=match may not be exhaustive")
    val subscriptionFqrs = subFqrs(projectId, sub)

    val restartSettings = RestartSettings(
      minBackoff = 100.millis,
      maxBackoff = 10.seconds,
      randomFactor = 0.2)

    GooglePubSub
      .subscribe(subscribe(projectId, sub), 1.second, restartSettings)
      .via(GooglePubSub.modifyAckDeadlineDynamic(subscriptionFqrs) { msg =>
        val sizeBytes = msg.message.map(_.data.size()).getOrElse(0)
        if (sizeBytes > 1024 * 1024) 600 // large messages: 10 min
        else if (sizeBytes > 1024) 120 // medium messages: 2 min
        else 30 // small messages: 30s
      })
      .map { msg =>
        val deadline = msg.message.map(_.data.size()).getOrElse(0) match {
          case s if s > 1024 * 1024 => "10min"
          case s if s > 1024        => "2min"
          case _                    => "30s"
        }
        println(s"[deadline=$deadline] ${msg.message.map(_.data.toStringUtf8.take(80)).getOrElse("")}")
        AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId))
      }
      .to(GooglePubSub.acknowledge(parallelism = 1))
      .mapMaterializedValue(Future.successful(_))
      .run()
  }

  /**
   * Subscribe with adaptive deadline estimation — the ack extension deadline
   * automatically adapts to the 99th percentile of observed processing times,
   * matching the behavior of Google's official client library.
   *
   * Usage: subscribe-adaptive <projectId> <subscription>
   */
  private def subscribeAdaptive(args: List[String])(implicit system: ActorSystem) = {
    import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineDistribution

    val projectId :: sub :: Nil = args: @nowarn("msg=match may not be exhaustive")
    val subscriptionFqrs = subFqrs(projectId, sub)

    val restartSettings = RestartSettings(
      minBackoff = 100.millis,
      maxBackoff = 10.seconds,
      randomFactor = 0.2)

    val dist = AckDeadlineDistribution(initialDeadlineSeconds = 10)

    GooglePubSub
      .subscribe(subscribe(projectId, sub), 1.second, restartSettings)
      .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, dist))
      .map { msg =>
        println(s"[adaptive-deadline=${dist.currentDeadlineSeconds}s] " +
          s"${msg.message.map(_.data.toStringUtf8).getOrElse("")}")
        AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId))
      }
      .to(GooglePubSub.acknowledge(parallelism = 1, dist))
      .mapMaterializedValue(Future.successful(_))
      .run()
  }

  /** Simulate message processing that can fail. */
  private def processMessage(msg: ReceivedMessage): Future[ReceivedMessage] = {
    val data = msg.message.map(_.data.toStringUtf8).getOrElse("")
    println(s"Processing: $data")
    if (data.contains("FAIL")) Future.failed(new RuntimeException(s"Failed to process: $data"))
    else Future.successful(msg)
  }

  private def publish(projectId: String, topic: String)(msg: String) =
    PublishRequest(topicFqrs(projectId, topic), Seq(PubsubMessage(ByteString.copyFromUtf8(msg))))

  private def subscribe(projectId: String, sub: String) =
    StreamingPullRequest(subFqrs(projectId, sub)).withStreamAckDeadlineSeconds(10)

  private def topicFqrs(projectId: String, topic: String) =
    s"projects/$projectId/topics/$topic"

  private def subFqrs(projectId: String, sub: String) =
    s"projects/$projectId/subscriptions/$sub"

}
