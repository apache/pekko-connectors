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

package org.apache.pekko.stream.connectors.kinesis.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.kinesis.ShardSettings;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class KinesisTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;
  private static ShardSettings settings;
  private static KinesisAsyncClient amazonKinesisAsync;

  @BeforeClass
  public static void setup() throws Exception {
    System.setProperty("aws.accessKeyId", "someKeyId");
    System.setProperty("aws.secretKey", "someSecretKey");

    system = ActorSystem.create();

    settings = ShardSettings.create("my-stream", "shard-id");
    amazonKinesisAsync = mock(KinesisAsyncClient.class);
  }

  @AfterClass
  public static void afterAll() {
    TestKit.shutdownActorSystem(system);
  }

  //  @Ignore("This test appears to trigger a deadlock, see
  // https://github.com/akka/alpakka/issues/390")
  @Test
  public void PullRecord() throws Exception {

    when(amazonKinesisAsync.describeStream((DescribeStreamRequest) any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                DescribeStreamResponse.builder()
                    .streamDescription(
                        StreamDescription.builder()
                            .shards(Shard.builder().shardId("id").build())
                            .hasMoreShards(false)
                            .build())
                    .build()));
    when(amazonKinesisAsync.getShardIterator((GetShardIteratorRequest) any()))
        .thenAnswer(
            (Answer)
                invocation ->
                    CompletableFuture.completedFuture(GetShardIteratorResponse.builder().build()));

    when(amazonKinesisAsync.getRecords((GetRecordsRequest) any()))
        .thenAnswer(
            (Answer)
                invocation ->
                    CompletableFuture.completedFuture(
                        GetRecordsResponse.builder()
                            .records(Record.builder().sequenceNumber("1").build())
                            .nextShardIterator("iter")
                            .build()));

    final Source<Record, NotUsed> source = KinesisSource.basic(settings, amazonKinesisAsync);
    final CompletionStage<Record> record = source.runWith(Sink.head(), system);

    assertEquals("1", record.toCompletableFuture().get(10, TimeUnit.SECONDS).sequenceNumber());
  }
}
