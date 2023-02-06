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

final class SqsPublishSettings private (val maxInFlight: Int) {
  require(maxInFlight > 0)

  def withMaxInFlight(maxInFlight: Int): SqsPublishSettings = copy(maxInFlight = maxInFlight)

  private def copy(maxInFlight: Int) = new SqsPublishSettings(maxInFlight)

  override def toString: String =
    "SqsPublishSettings(" +
    s"maxInFlight=$maxInFlight" +
    ")"
}

object SqsPublishSettings {
  val Defaults = new SqsPublishSettings(maxInFlight = 10)

  /**
   * Scala API
   */
  def apply(): SqsPublishSettings = Defaults

  /**
   * Java API
   */
  def create(): SqsPublishSettings = Defaults
}
