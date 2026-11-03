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

package org.apache.pekko.stream.connectors.kinesis.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.NotUsed;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.kinesis.CommittableRecord;
import org.apache.pekko.stream.connectors.kinesis.KinesisSchedulerCheckpointSettings;
import org.apache.pekko.stream.connectors.kinesis.KinesisSchedulerSourceSettings;
import org.apache.pekko.stream.connectors.kinesis.ShardIterators;
import org.apache.pekko.stream.connectors.kinesis.ShardSettings;
import org.apache.pekko.stream.connectors.kinesisfirehose.javadsl.KinesisFirehoseFlow;
import org.apache.pekko.stream.connectors.kinesisfirehose.javadsl.KinesisFirehoseSink;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.javadsl.SubSource;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;
import software.amazon.awssdk.services.firehose.model.Record;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import software.amazon.kinesis.coordinator.Scheduler;

/** Exercises the Java API of the Kinesis settings and stage factories without Kinesis. */
public class KinesisJavaApiTest {

  @Test
  public void shardSettingsGetters() {
    Instant timestamp = Instant.ofEpochSecond(1000);
    ShardSettings settings =
        ShardSettings.create("stream", "shard")
            .withShardIterator(ShardIterators.atTimestamp(timestamp))
            .withRefreshInterval(Duration.ofSeconds(2));
    assertEquals(Optional.of(timestamp), settings.getAtTimestamp());
    assertEquals(Optional.empty(), settings.getStartingSequenceNumber());
    assertEquals(Duration.ofSeconds(2), settings.getRefreshInterval());
  }

  @Test
  public void schedulerSettingsGetters() {
    assertEquals(
        Duration.ofSeconds(3),
        KinesisSchedulerSourceSettings.create(10, Duration.ofSeconds(3)).getBackpressureTimeout());
    assertEquals(
        Duration.ofSeconds(4),
        KinesisSchedulerCheckpointSettings.create(10, Duration.ofSeconds(4)).getMaxBatchWait());
  }

  /**
   * Compile-only check: the scheduler builder can be a lambda and shutdownReason is an Optional.
   */
  @SuppressWarnings("unused")
  private static void schedulerSourceCompiles(
      Scheduler scheduler, KinesisSchedulerSourceSettings settings, CommittableRecord record) {
    Source<CommittableRecord, CompletionStage<Scheduler>> source =
        KinesisSchedulerSource.create(factory -> scheduler, settings);
    SubSource<CommittableRecord, CompletionStage<Scheduler>> sharded =
        KinesisSchedulerSource.createSharded(factory -> scheduler, settings);
    Optional<software.amazon.kinesis.lifecycle.ShutdownReason> reason = record.getShutdownReason();
  }

  /** Compile-only check: partition/data variants use Pair and firehose stages use create. */
  @SuppressWarnings("unused")
  private static void stagesCompile(KinesisAsyncClient client, FirehoseAsyncClient firehose) {
    Flow<Pair<String, ByteBuffer>, PutRecordsResultEntry, NotUsed> data =
        KinesisFlow.byPartitionAndData("stream", client);
    Flow<Pair<String, ByteString>, PutRecordsResultEntry, NotUsed> bytes =
        KinesisFlow.byPartitionAndBytes("stream", client);
    Sink<Pair<String, ByteBuffer>, NotUsed> dataSink =
        KinesisSink.byPartitionAndData("stream", client);
    Sink<Pair<String, ByteString>, NotUsed> bytesSink =
        KinesisSink.byPartitionAndBytes("stream", client);
    Flow<Record, PutRecordBatchResponseEntry, NotUsed> firehoseFlow =
        KinesisFirehoseFlow.create("stream", firehose);
    Sink<Record, NotUsed> firehoseSink = KinesisFirehoseSink.create("stream", firehose);
  }
}
