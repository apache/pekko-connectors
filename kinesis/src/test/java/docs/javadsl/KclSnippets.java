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
import org.apache.pekko.stream.connectors.kinesis.CommittableRecord;
import org.apache.pekko.stream.connectors.kinesis.KinesisSchedulerCheckpointSettings;
import org.apache.pekko.stream.connectors.kinesis.KinesisSchedulerSourceSettings;
import org.apache.pekko.stream.connectors.kinesis.javadsl.KinesisSchedulerSource;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Source;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

public class KclSnippets {

  // #scheduler-settings
  final KinesisSchedulerSource.SchedulerBuilder schedulerBuilder =
      new KinesisSchedulerSource.SchedulerBuilder() {
        @Override
        public Scheduler build(ShardRecordProcessorFactory r) {
          return null; // build your own Scheduler here
        }
      };
  final KinesisSchedulerSourceSettings schedulerSettings =
      KinesisSchedulerSourceSettings.create(1000, Duration.of(1L, ChronoUnit.SECONDS));
  // #scheduler-settings

  // #scheduler-source
  final Source<CommittableRecord, CompletionStage<Scheduler>> schedulerSource =
      KinesisSchedulerSource.create(schedulerBuilder, schedulerSettings);
  // #scheduler-source

  // #checkpoint
  final KinesisSchedulerCheckpointSettings checkpointSettings =
      KinesisSchedulerCheckpointSettings.create(1000, Duration.of(30L, ChronoUnit.SECONDS));
  final Flow<CommittableRecord, KinesisClientRecord, NotUsed> checkpointFlow =
      KinesisSchedulerSource.checkpointRecordsFlow(checkpointSettings);
  // #checkpoint

}
