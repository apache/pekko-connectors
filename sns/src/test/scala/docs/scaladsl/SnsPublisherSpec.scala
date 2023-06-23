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
import pekko.stream.connectors.sns.IntegrationTestContext
import pekko.stream.connectors.sns.scaladsl.SnsPublisher
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Sink, Source }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sns.model.PublishRequest

import scala.concurrent.Future
import scala.concurrent.duration._

class SnsPublisherSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with IntegrationTestContext
    with LogCapturing {

  implicit val defaultPatience =
    PatienceConfig(timeout = 15.seconds, interval = 100.millis)

  "SNS Publisher sink" should "send string message" in {
    val published: Future[Done] =
      // #use-sink
      Source
        .single("message")
        .runWith(SnsPublisher.sink(topicArn))

    // #use-sink
    published.futureValue should be(Done)
  }

  it should "send publish request" in {
    val published: Future[Done] =
      // #use-sink
      Source
        .single(PublishRequest.builder().message("message").build())
        .runWith(SnsPublisher.publishSink(topicArn))

    // #use-sink
    published.futureValue should be(Done)
  }

  it should "send publish request with dynamic arn" in {
    val published: Future[Done] =
      // #use-sink
      Source
        .single(PublishRequest.builder().message("message").topicArn(topicArn).build())
        .runWith(SnsPublisher.publishSink())
    // #use-sink
    published.futureValue should be(Done)
  }

  "SNS Publisher flow" should "send string message" in {
    val published: Future[Done] =
      // #use-flow
      Source
        .single("message")
        .via(SnsPublisher.flow(topicArn))
        .runWith(Sink.foreach(res => println(res.messageId())))

    // #use-flow
    published.futureValue should be(Done)
  }

  it should "send publish request" in {
    val published: Future[Done] =
      // #use-flow
      Source
        .single(PublishRequest.builder().message("message").build())
        .via(SnsPublisher.publishFlow(topicArn))
        .runWith(Sink.foreach(res => println(res.messageId())))

    // #use-flow
    published.futureValue should be(Done)
  }

  it should "send publish request with dynamic topic" in {
    val published: Future[Done] =
      // #use-flow
      Source
        .single(PublishRequest.builder().message("message").topicArn(topicArn).build())
        .via(SnsPublisher.publishFlow())
        .runWith(Sink.foreach(res => println(res.messageId())))
    // #use-flow
    published.futureValue should be(Done)
  }

}
