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

package org.apache.pekko.stream.connectors.sqs

final class SqsPublishBatchSettings private (val concurrentRequests: Int) {

  def withConcurrentRequests(value: Int): SqsPublishBatchSettings = copy(concurrentRequests = value)

  private def copy(concurrentRequests: Int): SqsPublishBatchSettings =
    new SqsPublishBatchSettings(concurrentRequests = concurrentRequests)

  override def toString =
    s"""SqsPublishBatchSettings(concurrentRequests=$concurrentRequests)"""

}

object SqsPublishBatchSettings {

  val Defaults = new SqsPublishBatchSettings(
    concurrentRequests = 1)

  /** Scala API */
  def apply(): SqsPublishBatchSettings = Defaults

  /** Java API */
  def create(): SqsPublishBatchSettings = Defaults
}
