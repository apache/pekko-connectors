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

package org.apache.pekko.stream.connectors.kinesis.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.kinesis.KinesisErrors.NoShardsError
import pekko.stream.connectors.kinesis.ShardSettings
import pekko.stream.connectors.kinesis.impl.KinesisSourceStage
import pekko.stream.scaladsl.{ Merge, Source }
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.Record

object KinesisSource {

  /**
   * Read from one shard into a stream.
   */
  def basic(shardSettings: ShardSettings, amazonKinesisAsync: KinesisAsyncClient): Source[Record, NotUsed] = {
    KinesisFlow.checkClient(amazonKinesisAsync)
    Source.fromGraph(new KinesisSourceStage(shardSettings, amazonKinesisAsync))
  }

  /**
   * Read from multiple shards into a single stream.
   */
  def basicMerge(shardSettings: List[ShardSettings],
      amazonKinesisAsync: KinesisAsyncClient): Source[Record, NotUsed] = {
    require(shardSettings.nonEmpty, "shard settings need to be specified")
    val create: ShardSettings => Source[Record, NotUsed] = basic(_, amazonKinesisAsync)
    shardSettings match {
      case Nil                    => Source.failed(NoShardsError)
      case first :: Nil           => create(first)
      case first :: second :: Nil => Source.combine(create(first), create(second))(Merge(_))
      case first :: second :: rest =>
        Source.combine(create(first), create(second), rest.map(create(_)): _*)(Merge(_))
    }
  }

}
