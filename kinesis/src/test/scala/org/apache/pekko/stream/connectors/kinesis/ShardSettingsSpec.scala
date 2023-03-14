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

import java.time.Instant

import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

class ShardSettingsSpec extends AnyWordSpec with Matchers with LogCapturing {
  val baseSettings = ShardSettings("name", "shardid")
  "ShardSettings" must {
    "require a valid limit" in {
      a[IllegalArgumentException] should be thrownBy baseSettings.withLimit(10001)
      a[IllegalArgumentException] should be thrownBy baseSettings.withLimit(-1)
    }
    "accept a valid limit" in {
      noException should be thrownBy baseSettings.withLimit(500)
    }

    "accept all combinations of alterations with ShardIterator" in {
      noException should be thrownBy baseSettings
        .withShardIterator(ShardIterator.AtSequenceNumber("SQC"))
        .withShardIterator(ShardIterator.AfterSequenceNumber("SQC"))
        .withShardIterator(ShardIterator.AtTimestamp(Instant.EPOCH))
        .withShardIterator(ShardIterator.Latest)
        .withShardIterator(ShardIterator.TrimHorizon)
    }
  }
}
