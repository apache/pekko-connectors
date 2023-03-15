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

package docs.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.stream.connectors.kinesisfirehose.KinesisFirehoseFlowSettings
import pekko.stream.connectors.kinesisfirehose.scaladsl.{ KinesisFirehoseFlow, KinesisFirehoseSink }
import pekko.stream.scaladsl.{ Flow, Sink }
import software.amazon.awssdk.services.firehose.model.{ PutRecordBatchResponseEntry, Record }

object KinesisFirehoseSnippets {

  // #init-client
  import com.github.pjfanning.pekkohttpspi.PekkoHttpClient
  import software.amazon.awssdk.services.firehose.FirehoseAsyncClient

  implicit val system: ActorSystem = ActorSystem()

  implicit val amazonKinesisFirehoseAsync: software.amazon.awssdk.services.firehose.FirehoseAsyncClient =
    FirehoseAsyncClient
      .builder()
      .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
      // Possibility to configure the retry policy
      // see https://doc.akka.io/docs/alpakka/current/aws-shared-configuration.html
      // .overrideConfiguration(...)
      .build()

  system.registerOnTermination(amazonKinesisFirehoseAsync.close())
  // #init-client

  // #flow-settings
  val flowSettings = KinesisFirehoseFlowSettings
    .create()
    .withParallelism(1)
    .withMaxBatchSize(500)
    .withMaxRecordsPerSecond(5000)
    .withMaxBytesPerSecond(4000000)

  val defaultFlowSettings = KinesisFirehoseFlowSettings.Defaults
  // #flow-settings

  // #flow-sink
  val flow1: Flow[Record, PutRecordBatchResponseEntry, NotUsed] = KinesisFirehoseFlow("myStreamName")

  val flow2: Flow[Record, PutRecordBatchResponseEntry, NotUsed] = KinesisFirehoseFlow("myStreamName", flowSettings)

  val sink1: Sink[Record, NotUsed] = KinesisFirehoseSink("myStreamName")
  val sink2: Sink[Record, NotUsed] = KinesisFirehoseSink("myStreamName", flowSettings)
  // #flow-sink

  // #error-handling
  val flowWithErrors: Flow[Record, PutRecordBatchResponseEntry, NotUsed] = KinesisFirehoseFlow("streamName")
    .map { response =>
      if (response.errorCode() != null) {
        throw new RuntimeException(response.errorCode())
      }
      response
    }
  // #error-handling

}
