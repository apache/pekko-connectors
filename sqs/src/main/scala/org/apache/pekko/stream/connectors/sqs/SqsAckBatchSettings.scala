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

final class SqsAckBatchSettings private (val concurrentRequests: Int) {

  require(concurrentRequests > 0)

  def withConcurrentRequests(value: Int): SqsAckBatchSettings = copy(concurrentRequests = value)

  private def copy(concurrentRequests: Int): SqsAckBatchSettings =
    new SqsAckBatchSettings(concurrentRequests = concurrentRequests)

  override def toString =
    s"""SqsAckBatchSettings(concurrentRequests=$concurrentRequests)"""

}
object SqsAckBatchSettings {
  val Defaults = new SqsAckBatchSettings(
    concurrentRequests = 1)

  /** Scala API */
  def apply(): SqsAckBatchSettings = Defaults

  /** Java API */
  def create(): SqsAckBatchSettings = Defaults
}
