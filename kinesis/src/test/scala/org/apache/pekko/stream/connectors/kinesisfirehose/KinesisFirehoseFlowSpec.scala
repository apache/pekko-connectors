/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.kinesisfirehose

import java.util.concurrent.CompletableFuture

import org.apache.pekko.stream.connectors.kinesisfirehose.KinesisFirehoseErrors.FailurePublishingRecords
import org.apache.pekko.stream.connectors.kinesisfirehose.scaladsl.KinesisFirehoseFlow
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.apache.pekko.stream.testkit.scaladsl.{ TestSink, TestSource }
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.firehose.model._

import scala.jdk.CollectionConverters._

class KinesisFirehoseFlowSpec extends AnyWordSpec with Matchers with KinesisFirehoseMock with LogCapturing {

  "KinesisFirehoseFlow" must {

    "publish records" in assertAllStagesStopped {
      new Settings with KinesisFirehoseFlowProbe with WithPutRecordsSuccess {
        sourceProbe.sendNext(record)
        sourceProbe.sendNext(record)
        sourceProbe.sendNext(record)
        sourceProbe.sendNext(record)
        sourceProbe.sendNext(record)

        sinkProbe.requestNext() shouldBe publishedRecord
        sinkProbe.requestNext() shouldBe publishedRecord
        sinkProbe.requestNext() shouldBe publishedRecord
        sinkProbe.requestNext() shouldBe publishedRecord
        sinkProbe.requestNext() shouldBe publishedRecord

        sourceProbe.sendComplete()
        sinkProbe.expectComplete()
      }
    }

    "fails when request returns an error" in assertAllStagesStopped {
      new Settings with KinesisFirehoseFlowProbe with WithPutRecordsFailure {
        sourceProbe.sendNext(record)

        sinkProbe.request(1)
        sinkProbe.expectError(FailurePublishingRecords(requestError))
      }
    }
  }

  sealed trait Settings {
    val settings: KinesisFirehoseFlowSettings = KinesisFirehoseFlowSettings.Defaults
  }

  trait KinesisFirehoseFlowProbe { self: Settings =>
    val streamName = "stream-name"
    val record =
      Record.builder().data(SdkBytes.fromByteBuffer(ByteString("data").asByteBuffer)).build()
    val publishedRecord = PutRecordBatchResponseEntry.builder().build()
    val failingRecord =
      PutRecordBatchResponseEntry.builder().errorCode("error-code").errorMessage("error-message").build()
    val requestError = new RuntimeException("kinesisfirehose-error")

    val (sourceProbe, sinkProbe) =
      TestSource
        .probe[Record]
        .via(KinesisFirehoseFlow(streamName, settings))
        .toMat(TestSink.probe)(Keep.both)
        .run()
  }

  trait WithPutRecordsSuccess { self: KinesisFirehoseFlowProbe =>
    when(amazonKinesisFirehoseAsync.putRecordBatch(any[PutRecordBatchRequest])).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock) = {
        val request = invocation
          .getArgument[PutRecordBatchRequest](0)
        val result = PutRecordBatchResponse
          .builder()
          .failedPutCount(0)
          .requestResponses(request.records.asScala.map(_ => publishedRecord).asJava)
          .build()
        CompletableFuture.completedFuture(result)
      }
    })
  }

  trait WithPutRecordsFailure { self: KinesisFirehoseFlowProbe =>
    when(amazonKinesisFirehoseAsync.putRecordBatch(any[PutRecordBatchRequest])).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock) = {
        val future = new CompletableFuture()
        future.completeExceptionally(requestError)
        future
      }
    })
  }

}
