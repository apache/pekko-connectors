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

package docs.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.kinesisfirehose.KinesisFirehoseFlowSettings;
import org.apache.pekko.stream.connectors.kinesisfirehose.javadsl.KinesisFirehoseFlow;
import org.apache.pekko.stream.connectors.kinesisfirehose.javadsl.KinesisFirehoseSink;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
// #init-client
import com.github.pjfanning.pekkohttpspi.PekkoHttpClient;
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient;
// #init-client
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;
import software.amazon.awssdk.services.firehose.model.Record;

public class KinesisFirehoseSnippets {

  public void snippets() {
    // #init-client

    final ActorSystem system = ActorSystem.create();

    final software.amazon.awssdk.services.firehose.FirehoseAsyncClient amazonFirehoseAsync =
        FirehoseAsyncClient.builder()
            .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
            // Possibility to configure the retry policy
            // see https://pekko.apache.org/docs/pekko-connectors/current/aws-shared-configuration.html
            // .overrideConfiguration(...)
            .build();

    system.registerOnTermination(amazonFirehoseAsync::close);
    // #init-client

    // #flow-settings
    final KinesisFirehoseFlowSettings flowSettings =
        KinesisFirehoseFlowSettings.create()
            .withParallelism(1)
            .withMaxBatchSize(500)
            .withMaxRecordsPerSecond(1_000)
            .withMaxBytesPerSecond(1_000_000)
            .withMaxRecordsPerSecond(5);

    final KinesisFirehoseFlowSettings defaultFlowSettings = KinesisFirehoseFlowSettings.create();
    // #flow-settings

    // #flow-sink
    final Flow<Record, PutRecordBatchResponseEntry, NotUsed> flow =
        KinesisFirehoseFlow.apply("streamName", flowSettings, amazonFirehoseAsync);

    final Flow<Record, PutRecordBatchResponseEntry, NotUsed> defaultSettingsFlow =
        KinesisFirehoseFlow.apply("streamName", amazonFirehoseAsync);

    final Sink<Record, NotUsed> sink =
        KinesisFirehoseSink.apply("streamName", flowSettings, amazonFirehoseAsync);

    final Sink<Record, NotUsed> defaultSettingsSink =
        KinesisFirehoseSink.apply("streamName", amazonFirehoseAsync);
    // #flow-sink

    // #error-handling
    final Flow<Record, PutRecordBatchResponseEntry, NotUsed> flowWithErrors =
        KinesisFirehoseFlow.apply("streamName", flowSettings, amazonFirehoseAsync)
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
