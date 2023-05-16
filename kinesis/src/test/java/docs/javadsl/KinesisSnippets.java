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

package docs.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.kinesis.KinesisFlowSettings;
import org.apache.pekko.stream.connectors.kinesis.ShardIterators;
import org.apache.pekko.stream.connectors.kinesis.ShardSettings;
import org.apache.pekko.stream.connectors.kinesis.javadsl.KinesisFlow;
import org.apache.pekko.stream.connectors.kinesis.javadsl.KinesisSink;
import org.apache.pekko.stream.connectors.kinesis.javadsl.KinesisSource;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.FlowWithContext;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
// #init-client
import com.github.pjfanning.pekkohttpspi.PekkoHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
// #init-client
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import software.amazon.awssdk.services.kinesis.model.Record;
// #source-settings
// #source-settings

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class KinesisSnippets {

  public void snippets() {
    // #init-client

    final ActorSystem system = ActorSystem.create();

    final software.amazon.awssdk.services.kinesis.KinesisAsyncClient amazonKinesisAsync =
        KinesisAsyncClient.builder()
            .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
            // Possibility to configure the retry policy
            // see https://pekko.apache.org/docs/pekko-connectors/current/aws-shared-configuration.html
            // .overrideConfiguration(...)
            .build();

    system.registerOnTermination(amazonKinesisAsync::close);
    // #init-client

    // #source-settings
    final ShardSettings settings =
        ShardSettings.create("streamName", "shard-id")
            .withRefreshInterval(Duration.ofSeconds(1))
            .withLimit(500)
            .withShardIterator(ShardIterators.trimHorizon());
    // #source-settings

    // #source-single
    final Source<software.amazon.awssdk.services.kinesis.model.Record, NotUsed> source =
        KinesisSource.basic(settings, amazonKinesisAsync);
    // #source-single

    // #source-list
    final List<ShardSettings> mergeSettings =
        Arrays.asList(
            ShardSettings.create("streamName", "shard-id-1"),
            ShardSettings.create("streamName", "shard-id-2"));
    final Source<Record, NotUsed> two = KinesisSource.basicMerge(mergeSettings, amazonKinesisAsync);
    // #source-list

    // #flow-settings
    final KinesisFlowSettings flowSettings =
        KinesisFlowSettings.create()
            .withParallelism(1)
            .withMaxBatchSize(500)
            .withMaxRecordsPerSecond(1_000)
            .withMaxBytesPerSecond(1_000_000)
            .withMaxRecordsPerSecond(5);

    final KinesisFlowSettings defaultFlowSettings = KinesisFlowSettings.create();

    final KinesisFlowSettings fourShardFlowSettings = KinesisFlowSettings.byNumberOfShards(4);
    // #flow-settings

    // #flow-sink
    final Flow<PutRecordsRequestEntry, PutRecordsResultEntry, NotUsed> flow =
        KinesisFlow.create("streamName", flowSettings, amazonKinesisAsync);

    final Flow<PutRecordsRequestEntry, PutRecordsResultEntry, NotUsed> defaultSettingsFlow =
        KinesisFlow.create("streamName", amazonKinesisAsync);

    final FlowWithContext<PutRecordsRequestEntry, String, PutRecordsResultEntry, String, NotUsed>
        flowWithStringContext =
            KinesisFlow.createWithContext("streamName", flowSettings, amazonKinesisAsync);

    final FlowWithContext<PutRecordsRequestEntry, String, PutRecordsResultEntry, String, NotUsed>
        defaultSettingsFlowWithStringContext =
            KinesisFlow.createWithContext("streamName", flowSettings, amazonKinesisAsync);

    final Sink<PutRecordsRequestEntry, NotUsed> sink =
        KinesisSink.create("streamName", flowSettings, amazonKinesisAsync);

    final Sink<PutRecordsRequestEntry, NotUsed> defaultSettingsSink =
        KinesisSink.create("streamName", amazonKinesisAsync);
    // #flow-sink

    // #error-handling
    final Flow<PutRecordsRequestEntry, PutRecordsResultEntry, NotUsed> flowWithErrors =
        KinesisFlow.create("streamName", flowSettings, amazonKinesisAsync)
            .map(
                response -> {
                  if (response.errorCode() != null) {
                    throw new RuntimeException(response.errorCode());
                  }

                  return response;
                });
    // #error-handling
  }
}
