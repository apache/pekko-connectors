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

import java.util.concurrent.TimeUnit

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.sqs._
import pekko.stream.connectors.sqs.scaladsl._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.util.ccompat.JavaConverters._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{ Message, ReceiveMessageRequest, SendMessageRequest }

import scala.concurrent.duration._

class SqsPublishSpec extends AnyFlatSpec with Matchers with DefaultTestContext with LogCapturing {

  abstract class IntegrationFixture(fifo: Boolean = false) {
    val queueUrl: String = if (fifo) randomFifoQueueUrl() else randomQueueUrl()
    implicit val awsSqsClient: SqsAsyncClient = sqsClient

    def receiveMessage(): Message =
      awsSqsClient
        .receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build())
        .get(2, TimeUnit.SECONDS)
        .messages()
        .asScala
        .head

    def receiveMessages(maxNumberOfMessages: Int): Seq[Message] = {
      // see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/sqs/model/ReceiveMessageRequest.html
      require(maxNumberOfMessages > 0 && maxNumberOfMessages <= 10, "maxNumberOfMessages must be in 1 to 10")

      val request =
        ReceiveMessageRequest
          .builder()
          .queueUrl(queueUrl)
          .maxNumberOfMessages(maxNumberOfMessages)
          .build()

      awsSqsClient.receiveMessage(request).get(2, TimeUnit.SECONDS).messages().asScala.toSeq
    }
  }

  "SqsPublishSettings" should "construct settings" in {
    // #SqsPublishSettings
    val sinkSettings =
      SqsPublishSettings()
        .withMaxInFlight(10)
    // #SqsPublishSettings
    sinkSettings.maxInFlight shouldBe 10
  }

  it should "require valid maxInFlight" in {
    a[IllegalArgumentException] should be thrownBy {
      SqsPublishSettings().withMaxInFlight(0)
    }
  }

  it should "accept valid parameters" in {
    SqsPublishSettings().withMaxInFlight(1)
  }

  "SqsPublishBatchSettings" should "construct settings" in {
    // #SqsPublishBatchSettings
    val batchSettings =
      SqsPublishBatchSettings()
        .withConcurrentRequests(1)
    // #SqsPublishBatchSettings
    batchSettings.concurrentRequests shouldBe 1
  }

  "SqsPublishGroupedSettings" should "construct settings" in {
    // #SqsPublishGroupedSettings
    val batchSettings =
      SqsPublishGroupedSettings()
        .withMaxBatchSize(10)
        .withMaxBatchWait(500.millis)
        .withConcurrentRequests(1)
    // #SqsPublishGroupedSettings
    batchSettings.concurrentRequests shouldBe 1
  }

  "PublishSink" should "publish and pull a message" taggedAs Integration in {
    new IntegrationFixture {
      val future =
        // #run-string
        Source
          .single("connectors")
          .runWith(SqsPublishSink(queueUrl))
      // #run-string
      future.futureValue shouldBe Done

      receiveMessage().body() shouldBe "connectors"
    }
  }

  it should "publish and pull a message provided as a SendMessageRequest" taggedAs Integration in {
    new IntegrationFixture {
      val future =
        // #run-send-request
        // for fix SQS queue
        Source
          .single(SendMessageRequest.builder().messageBody("connectors").build())
          .runWith(SqsPublishSink.messageSink(queueUrl))

      // #run-send-request

      future.futureValue shouldBe Done

      receiveMessage().body() shouldBe "connectors"
    }
  }

  it should "publish and pull a message provided as a SendMessageRequest with dynamic queue" taggedAs Integration in {
    new IntegrationFixture {
      val future =
        // #run-send-request
        // for dynamic SQS queues
        Source
          .single(SendMessageRequest.builder().messageBody("connectors").queueUrl(queueUrl).build())
          .runWith(SqsPublishSink.messageSink())
      // #run-send-request

      future.futureValue shouldBe Done

      receiveMessage().body() shouldBe "connectors"
    }
  }

  it should "publish messages by grouping and pull them" taggedAs Integration in {
    new IntegrationFixture {
      // #group
      val messages = for (i <- 0 until 10) yield s"Message - $i"

      val future = Source(messages)
        .runWith(SqsPublishSink.grouped(queueUrl, SqsPublishGroupedSettings.Defaults.withMaxBatchSize(2)))
      // #group

      future.futureValue shouldBe Done

      receiveMessages(10) should have size 10
    }
  }

  it should "publish batch of messages and pull them" taggedAs Integration in {
    new IntegrationFixture {
      // #batch-string
      val messages = for (i <- 0 until 10) yield s"Message - $i"

      val future = Source
        .single(messages)
        .runWith(SqsPublishSink.batch(queueUrl))
      // #batch-string

      future.futureValue shouldBe Done

      receiveMessages(10) should have size 10
    }
  }

  it should "publish batch of SendMessageRequests and pull them" taggedAs Integration in {
    new IntegrationFixture {
      // #batch-send-request
      val messages = for (i <- 0 until 10) yield SendMessageRequest.builder().messageBody(s"Message - $i").build()

      val future = Source
        .single(messages)
        .runWith(SqsPublishSink.batchedMessageSink(queueUrl))
      // #batch-send-request

      future.futureValue shouldBe Done

      receiveMessages(10) should have size 10
    }
  }

  "PublishFlow" should "put message in a flow, then pass the result further" taggedAs Integration in {
    new IntegrationFixture {
      val future =
        // #flow
        // for fix SQS queue
        Source
          .single(SendMessageRequest.builder().messageBody("connectors").build())
          .via(SqsPublishFlow(queueUrl))
          .runWith(Sink.head)

      // #flow

      val result = future.futureValue
      result.result
      result.result.md5OfMessageBody() shouldBe md5HashString("connectors")

      receiveMessage().body() shouldBe "connectors"
    }
  }

  it should "put message in a flow, then pass the result further with dynamic queue" taggedAs Integration in {
    new IntegrationFixture {
      val future =
        // #flow
        // for dynamic SQS queues
        Source
          .single(SendMessageRequest.builder().messageBody("connectors").queueUrl(queueUrl).build())
          .via(SqsPublishFlow())
          .runWith(Sink.head)
      // #flow

      val result = future.futureValue
      result.result.md5OfMessageBody() shouldBe md5HashString("connectors")

      receiveMessage().body() shouldBe "connectors"
    }
  }

  ignore should "put message in a flow, then pass the result further with fifo queues" taggedAs Integration in new IntegrationFixture(
    fifo = true) {
    // elasticmq does not provide proper fifo support (see https://github.com/adamw/elasticmq/issues/92)
    // set your fifo sqs queue url and awsSqsClient manually
    // override val queueUrl = "https://sqs.us-east-1.amazonaws.com/$AWS_ACCOUNT_ID/$queue_name.fifo"
    // override implicit val awsSqsClient: SqsAsyncClient = SqsAsyncClient.builder().build()

    val future =
      Source
        .single(
          SendMessageRequest
            .builder()
            .messageBody("connectors")
            .messageGroupId("group-id")
            .messageDeduplicationId("deduplication-id")
            .build())
        .via(SqsPublishFlow(queueUrl))
        .runWith(Sink.head)

    val result = future.futureValue
    result.result.md5OfMessageBody() shouldBe md5HashString("connectors")
    result.result.sequenceNumber() should not be empty

    receiveMessage().body() shouldBe "connectors"
  }

  ignore should "put message in a flow, batch, then pass the result further with fifo queues" taggedAs Integration in new IntegrationFixture(
    fifo = true) {
    // elasticmq does not provide proper fifo support (see https://github.com/adamw/elasticmq/issues/92)
    // set your fifo sqs queue url and awsSqsClient manually
    // override val queueUrl = "https://sqs.us-east-1.amazonaws.com/$AWS_ACCOUNT_ID/$queue_name.fifo"
    // override implicit val awsSqsClient: SqsAsyncClient = SqsAsyncClient.builder().build()

    val messages =
      for (i <- 0 until 10)
        yield SendMessageRequest
          .builder()
          .messageBody(s"Message $i")
          .messageGroupId("group-id")
          .messageDeduplicationId(s"deduplication-$i")
          .build()

    val future =
      Source
        .single(messages)
        .via(SqsPublishFlow.batch(queueUrl))
        .runWith(Sink.seq)

    future.futureValue.flatten.zipWithIndex.foreach {
      case (result, i) =>
        result.result.md5OfMessageBody() shouldBe md5HashString(s"Message $i")
        result.result.sequenceNumber() should not be empty
    }

    receiveMessages(10) should have size 10
  }

  def md5HashString(s: String): String = {
    import java.security.MessageDigest
    val md = MessageDigest.getInstance("MD5")
    val bigInt = BigInt(1, md.digest(s.getBytes))
    f"$bigInt%032x"
  }
}
