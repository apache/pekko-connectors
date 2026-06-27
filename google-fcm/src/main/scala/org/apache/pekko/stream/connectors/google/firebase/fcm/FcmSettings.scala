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

package org.apache.pekko.stream.connectors.google.firebase.fcm

import org.apache.pekko

import java.util.Objects
import scala.annotation.nowarn

@nowarn("msg=deprecated")
final class FcmSettings private (
    val isTest: Boolean,
    val maxConcurrentConnections: Int) {

  def withIsTest(value: Boolean): FcmSettings = if (isTest == value) this else copy(isTest = value)

  def withMaxConcurrentConnections(value: Int): FcmSettings = copy(maxConcurrentConnections = value)

  private def copy(
      isTest: Boolean = isTest,
      maxConcurrentConnections: Int = maxConcurrentConnections): FcmSettings =
    new FcmSettings(isTest = isTest,
      maxConcurrentConnections = maxConcurrentConnections)

  override def toString =
    s"""FcmFlowConfig(isTest=$isTest,maxConcurrentConnections=$maxConcurrentConnections)"""
}

object FcmSettings {

  /** Scala API */
  def apply(): FcmSettings = new FcmSettings(
    isTest = false,
    maxConcurrentConnections = 100)

  /** Java API */
  def create(): FcmSettings = apply()
}
