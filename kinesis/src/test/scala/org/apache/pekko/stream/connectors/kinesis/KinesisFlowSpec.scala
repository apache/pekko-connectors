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

package org.apache.pekko.stream.connectors.kinesis

import java.util.concurrent.CompletableFuture

import org.apache.pekko
import pekko.stream.connectors.kinesis.KinesisErrors.FailurePublishingRecords
import pekko.stream.connectors.kinesis.scaladsl.KinesisFlow
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Keep
import pekko.stream.testkit.scaladsl.{ TestSink, TestSource }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.util.ccompat.JavaConverters._
import pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model._

class KinesisFlowSpec extends AnyWordSpec with Matchers with KinesisMock with LogCapturing {

  "KinesisFlow" must {

    "publish records" in assertAllStagesStopped {
      new Settings with KinesisFlowProbe with WithPutRecordsSuccess {
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

    "fail when request returns an error" in assertAllStagesStopped {
      new Settings with KinesisFlowProbe with WithPutRecordsFailure {
        sourceProbe.sendNext(record)

        sinkProbe.request(1)
        sinkProbe.expectError(FailurePublishingRecords(requestError))
      }
    }
  }

  "KinesisFlowWithUserContext" must {
    "return token in result" in assertAllStagesStopped {
      new Settings with KinesisFlowWithContextProbe with WithPutRecordsSuccess {
        val records = recordStream.take(5)
        records.foreach(sourceProbe.sendNext)
        val results = for (_ <- 1 to records.size) yield sinkProbe.requestNext()
        results shouldBe resultStream.take(records.size)

        sourceProbe.sendComplete()
        sinkProbe.expectComplete()
      }
    }
  }

  sealed trait Settings {
    val settings: KinesisFlowSettings = KinesisFlowSettings.Defaults
  }

  trait KinesisFlowProbe { self: Settings =>
    val streamName = "stream-name"
    val record =
      PutRecordsRequestEntry
        .builder()
        .partitionKey("partition-key")
        .data(SdkBytes.fromByteBuffer(ByteString("data").asByteBuffer))
        .build()

    val (sourceProbe, sinkProbe) =
      TestSource
        .probe[PutRecordsRequestEntry]
        .via(KinesisFlow(streamName, settings))
        .toMat(TestSink.probe[PutRecordsResultEntry])(Keep.both)
        .run()
  }

  trait KinesisFlowWithContextProbe { self: Settings =>
    val streamName = "stream-name"
    val recordStream = LazyList
      .from(1)
      .map(i =>
        (PutRecordsRequestEntry
            .builder()
            .partitionKey("partition-key")
            .data(SdkBytes.fromByteBuffer(ByteString(i).asByteBuffer))
            .build(),
          i))
    val resultStream = LazyList
      .from(1)
      .map(i => (PutRecordsResultEntry.builder().build(), i))

    val (sourceProbe, sinkProbe) =
      TestSource
        .probe[(PutRecordsRequestEntry, Int)]
        .via(KinesisFlow.withContext(streamName, settings))
        .toMat(TestSink.probe)(Keep.both)
        .run()
  }

  trait WithPutRecordsSuccess { self: Settings =>
    val publishedRecord = PutRecordsResultEntry.builder().build()
    when(amazonKinesisAsync.putRecords(any[PutRecordsRequest])).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock) = {
        val request = invocation
          .getArgument[PutRecordsRequest](0)
        val result = PutRecordsResponse
          .builder()
          .failedRecordCount(0)
          .records(request.records.asScala.map(_ => publishedRecord).asJava)
          .build()
        CompletableFuture.completedFuture(result)
      }
    })
  }

  trait WithPutRecordsFailure { self: Settings =>
    val requestError = new RuntimeException("kinesis-error")
    when(amazonKinesisAsync.putRecords(any[PutRecordsRequest])).thenReturn {
      val future = new CompletableFuture[PutRecordsResponse]()
      future.completeExceptionally(requestError)
      future
    }
  }
}
