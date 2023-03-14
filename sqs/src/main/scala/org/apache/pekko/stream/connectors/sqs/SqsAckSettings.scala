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

final class SqsAckSettings private (val maxInFlight: Int) {

  require(maxInFlight > 0)

  def withMaxInFlight(value: Int): SqsAckSettings = copy(maxInFlight = value)

  private def copy(maxInFlight: Int): SqsAckSettings =
    new SqsAckSettings(maxInFlight = maxInFlight)

  override def toString =
    s"""SqsAckSinkSettings(maxInFlight=$maxInFlight)"""
}

object SqsAckSettings {

  val Defaults = new SqsAckSettings(maxInFlight = 10)

  /** Scala API */
  def apply(): SqsAckSettings = Defaults

  /** Java API */
  def create(): SqsAckSettings = Defaults

  /** Scala API */
  def apply(
      maxInFlight: Int): SqsAckSettings = new SqsAckSettings(
    maxInFlight)

  /** Java API */
  def create(
      maxInFlight: Int): SqsAckSettings = new SqsAckSettings(
    maxInFlight)
}
