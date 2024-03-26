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

package org.apache.pekko.stream.connectors.sqs

import scala.concurrent.duration._

final class SqsPublishGroupedSettings private (val maxBatchSize: Int,
    val maxBatchWait: scala.concurrent.duration.FiniteDuration,
    val concurrentRequests: Int) {

  require(
    maxBatchSize > 0 && maxBatchSize <= 10,
    s"Invalid value for maxBatchSize: $maxBatchSize. It should be 0 < maxBatchSize < 10, due to the Amazon SQS requirements.")

  def withMaxBatchSize(value: Int): SqsPublishGroupedSettings = copy(maxBatchSize = value)

  /** Scala API */
  def withMaxBatchWait(value: scala.concurrent.duration.FiniteDuration): SqsPublishGroupedSettings =
    copy(maxBatchWait = value)

  /** Java API */
  def withMaxBatchWait(value: java.time.Duration): SqsPublishGroupedSettings =
    withMaxBatchWait(
      scala.concurrent.duration.FiniteDuration(value.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS))

  def withConcurrentRequests(value: Int): SqsPublishGroupedSettings = copy(concurrentRequests = value)

  private def copy(maxBatchSize: Int = maxBatchSize,
      maxBatchWait: scala.concurrent.duration.FiniteDuration = maxBatchWait,
      concurrentRequests: Int = concurrentRequests): SqsPublishGroupedSettings =
    new SqsPublishGroupedSettings(maxBatchSize = maxBatchSize,
      maxBatchWait = maxBatchWait,
      concurrentRequests = concurrentRequests)

  override def toString: String =
    "SqsPublishGroupedSettings(" +
    s"maxBatchSize=$maxBatchSize," +
    s"maxBatchWait=$maxBatchWait," +
    s"concurrentRequests=$concurrentRequests" +
    ")"

}

object SqsPublishGroupedSettings {

  val Defaults = new SqsPublishGroupedSettings(
    maxBatchSize = 10,
    maxBatchWait = 500.millis,
    concurrentRequests = 1)

  /** Scala API */
  def apply(): SqsPublishGroupedSettings = Defaults

  /** Java API */
  def create(): SqsPublishGroupedSettings = Defaults
}
