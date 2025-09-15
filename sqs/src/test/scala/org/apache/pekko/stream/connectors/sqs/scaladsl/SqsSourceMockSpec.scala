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

package org.apache.pekko.stream.connectors.sqs.scaladsl

import java.util.concurrent.CompletableFuture

import org.apache.pekko
import pekko.stream.connectors.sqs.SqsSourceSettings
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.testkit.scaladsl.TestSink
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{ atMost => atMostTimes, _ }
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{ Message, ReceiveMessageRequest, ReceiveMessageResponse }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.FutureConverters._

class SqsSourceMockSpec extends AnyFlatSpec with Matchers with DefaultTestContext with LogCapturing {

  override def createAsyncClient(sqsEndpoint: String): SqsAsyncClient = ???
  override def closeSqsClient(): Unit = ()
  val defaultMessages = (1 to 10).map { i =>
    Message.builder().body(s"message $i").build()
  }

  "SqsSource" should "send a request and unwrap the response" in {
    implicit val sqsClient: SqsAsyncClient = mock[SqsAsyncClient]
    when(sqsClient.receiveMessage(any[ReceiveMessageRequest]))
      .thenReturn(
        CompletableFuture.completedFuture(
          ReceiveMessageResponse
            .builder()
            .messages(defaultMessages: _*)
            .build()))

    val probe = SqsSource(
      "url",
      SqsSourceSettings.Defaults.withMaxBufferSize(10)).runWith(TestSink.probe[Message])

    defaultMessages.foreach(probe.requestNext)

    /**
     * Invocations:
     * 1 - to initially fill the buffer
     * 2 - asynchronous call after the buffer is filled
     * 3 - buffer proxies pull when it's not full -> async stage provides the data and executes the next call
     */
    verify(sqsClient, times(3)).receiveMessage(any[ReceiveMessageRequest])
    probe.cancel()
  }

  it should "buffer messages and acquire them fast with slow sqs" in {
    implicit val sqsClient: SqsAsyncClient = mock[SqsAsyncClient]
    val timeout = 1.second
    val bufferToBatchRatio = 5

    when(sqsClient.receiveMessage(any[ReceiveMessageRequest]))
      .thenAnswer(new Answer[CompletableFuture[ReceiveMessageResponse]] {
        def answer(invocation: InvocationOnMock) =
          pekko.pattern
            .after(timeout, system.scheduler) {
              Future.successful(
                ReceiveMessageResponse
                  .builder()
                  .messages(defaultMessages: _*)
                  .build())
            }(system.dispatcher)
            .asJava
            .toCompletableFuture
      })

    val probe = SqsSource(
      "url",
      SqsSourceSettings.Defaults.withMaxBufferSize(
        SqsSourceSettings.Defaults.maxBatchSize * bufferToBatchRatio)).runWith(TestSink.probe[Message])

    Thread.sleep(timeout.toMillis * (bufferToBatchRatio + 1))

    for {
      i <- 1 to bufferToBatchRatio
      message <- defaultMessages
    } {
      probe.requestNext() shouldEqual message
    }
    probe.cancel()
  }

  it should "enable throttling on emptyReceives and disable throttling when a new message arrives" in {
    implicit val sqsClient: SqsAsyncClient = mock[SqsAsyncClient]
    val firstWithDataCount = 30
    val thenEmptyCount = 15
    val parallelism = 10
    val timeout = 1.second
    var requestsCounter = 0
    when(sqsClient.receiveMessage(any[ReceiveMessageRequest]))
      .thenAnswer(new Answer[CompletableFuture[ReceiveMessageResponse]] {
        def answer(invocation: InvocationOnMock) = {
          requestsCounter += 1

          if (requestsCounter > firstWithDataCount && requestsCounter <= firstWithDataCount + thenEmptyCount) {
            pekko.pattern
              .after(timeout, system.scheduler) {
                Future.successful(
                  ReceiveMessageResponse
                    .builder()
                    .messages(List.empty[Message]: _*)
                    .build())
              }(system.dispatcher)
              .asJava
              .toCompletableFuture
          } else {
            CompletableFuture.completedFuture(
              ReceiveMessageResponse
                .builder()
                .messages(defaultMessages: _*)
                .build())
          }
        }
      })

    val probe = SqsSource(
      "url",
      SqsSourceSettings.Defaults
        .withMaxBufferSize(10)
        .withParallelRequests(10)
        .withWaitTime(timeout)).runWith(TestSink.probe[Message])

    (1 to firstWithDataCount * 10).foreach(_ => probe.requestNext())

    verify(sqsClient, atMostTimes(firstWithDataCount + parallelism)).receiveMessage(any[ReceiveMessageRequest])

    // now the throttling kicks in
    probe.request(1)
    probe.expectNoMessage((thenEmptyCount - parallelism + 1) * timeout)
    verify(sqsClient, atMostTimes(firstWithDataCount + thenEmptyCount)).receiveMessage(any[ReceiveMessageRequest])

    // now the throttling is off
    probe.expectNext()

    (1 to 1000).foreach(_ => probe.requestNext(10.milliseconds))
    probe.cancel()
  }
}
