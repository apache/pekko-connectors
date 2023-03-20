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

package org.apache.pekko.stream.connectors.kinesis.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.kinesis.{ scaladsl, ShardSettings }
import pekko.stream.javadsl.Source
import pekko.util.ccompat.JavaConverters._
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.Record

object KinesisSource {

  /**
   * Read from one shard into a stream.
   */
  def basic(shardSettings: ShardSettings, amazonKinesisAsync: KinesisAsyncClient): Source[Record, NotUsed] =
    scaladsl.KinesisSource.basic(shardSettings, amazonKinesisAsync).asJava

  /**
   * Read from multiple shards into a single stream.
   */
  def basicMerge(shardSettings: java.util.List[ShardSettings],
      amazonKinesisAsync: KinesisAsyncClient): Source[Record, NotUsed] =
    scaladsl.KinesisSource.basicMerge(shardSettings.asScala.toList, amazonKinesisAsync).asJava

}
