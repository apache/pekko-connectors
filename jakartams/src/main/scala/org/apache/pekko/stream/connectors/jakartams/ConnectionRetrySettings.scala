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

package org.apache.pekko.stream.connectors.jakartams

import com.typesafe.config.Config
import org.apache.pekko
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.util.JavaDurationConverters._

import scala.concurrent.duration._

/**
 * When a connection to a broker cannot be established and errors out, or is timing out being established or
 * started, the connection can be retried.
 * All JMS publishers, consumers, and browsers are configured with connection retry settings.
 */
final class ConnectionRetrySettings private (
    val connectTimeout: scala.concurrent.duration.FiniteDuration,
    val initialRetry: scala.concurrent.duration.FiniteDuration,
    val backoffFactor: Double,
    val maxBackoff: scala.concurrent.duration.FiniteDuration,
    val maxRetries: Int) {

  /** Time allowed to establish and start a connection. */
  def withConnectTimeout(timeout: scala.concurrent.duration.FiniteDuration): ConnectionRetrySettings =
    copy(connectTimeout = timeout)

  /** Java API: Time allowed to establish and start a connection. */
  def withConnectTimeout(timeout: java.time.Duration): ConnectionRetrySettings = copy(connectTimeout = timeout.asScala)

  /** Wait time before retrying the first time. */
  def withInitialRetry(timeout: scala.concurrent.duration.FiniteDuration): ConnectionRetrySettings =
    copy(initialRetry = timeout)

  /** Java API: Wait time before retrying the first time. */
  def withInitialRetry(timeout: java.time.Duration): ConnectionRetrySettings = copy(initialRetry = timeout.asScala)

  /** Back-off factor for subsequent retries. */
  def withBackoffFactor(factor: Double): ConnectionRetrySettings = copy(backoffFactor = factor)

  /** Maximum back-off time allowed, after which all retries will happen after this delay. */
  def withMaxBackoff(backoffTime: scala.concurrent.duration.FiniteDuration): ConnectionRetrySettings =
    copy(maxBackoff = backoffTime)

  /** Java API: Maximum back-off time allowed, after which all retries will happen after this delay. */
  def withMaxBackoff(backoffTime: java.time.Duration): ConnectionRetrySettings = copy(maxBackoff = backoffTime.asScala)

  /** Maximum number of retries allowed. */
  def withMaxRetries(value: Int): ConnectionRetrySettings = copy(maxRetries = value)

  /** Do not limit the number of retries. */
  def withInfiniteRetries(): ConnectionRetrySettings = withMaxRetries(ConnectionRetrySettings.infiniteRetries)

  /** The wait time before the next attempt may be made. */
  def waitTime(retryNumber: Int): FiniteDuration =
    (initialRetry * Math.pow(retryNumber, backoffFactor)).asInstanceOf[FiniteDuration].min(maxBackoff)

  private def copy(
      connectTimeout: scala.concurrent.duration.FiniteDuration = connectTimeout,
      initialRetry: scala.concurrent.duration.FiniteDuration = initialRetry,
      backoffFactor: Double = backoffFactor,
      maxBackoff: scala.concurrent.duration.FiniteDuration = maxBackoff,
      maxRetries: Int = maxRetries): ConnectionRetrySettings = new ConnectionRetrySettings(
    connectTimeout = connectTimeout,
    initialRetry = initialRetry,
    backoffFactor = backoffFactor,
    maxBackoff = maxBackoff,
    maxRetries = maxRetries)

  override def toString: String =
    "ConnectionRetrySettings(" +
    s"connectTimeout=${connectTimeout.toCoarsest}," +
    s"initialRetry=${initialRetry.toCoarsest}," +
    s"backoffFactor=$backoffFactor," +
    s"maxBackoff=${maxBackoff.toCoarsest}," +
    s"maxRetries=${if (maxRetries == ConnectionRetrySettings.infiniteRetries) "infinite" else maxRetries}" +
    ")"
}

object ConnectionRetrySettings {
  val configPath = "pekko.connectors.jakartams.connection-retry"

  val infiniteRetries: Int = -1

  /**
   * Reads from the given config.
   */
  def apply(c: Config): ConnectionRetrySettings = {
    val connectTimeout = c.getDuration("connect-timeout").asScala
    val initialRetry = c.getDuration("initial-retry").asScala
    val backoffFactor = c.getDouble("backoff-factor")
    val maxBackoff = c.getDuration("max-backoff").asScala
    val maxRetries = if (c.getString("max-retries") == "infinite") infiniteRetries else c.getInt("max-retries")
    new ConnectionRetrySettings(
      connectTimeout,
      initialRetry,
      backoffFactor,
      maxBackoff,
      maxRetries)
  }

  /** Java API: Reads from the given config. */
  def create(c: Config): ConnectionRetrySettings = apply(c)

  /**
   * Reads from the default config provided by the actor system at `pekko.connectors.jakartams.connection-retry`.
   *
   * @param actorSystem The actor system
   */
  def apply(actorSystem: ActorSystem): ConnectionRetrySettings =
    apply(actorSystem.settings.config.getConfig(configPath))

  /**
   * Reads from the default config provided by the actor system at `pekko.connectors.jakartams.connection-retry`.
   *
   * @param actorSystem The actor system
   */
  def apply(actorSystem: ClassicActorSystemProvider): ConnectionRetrySettings =
    apply(actorSystem.classicSystem.settings.config.getConfig(configPath))

  /**
   * Java API: Reads from the default config provided by the actor system at `pekko.connectors.jakartams.connection-retry`.
   *
   * @param actorSystem The actor system
   */
  def create(actorSystem: ActorSystem): ConnectionRetrySettings = apply(actorSystem)

  /**
   * Java API: Reads from the default config provided by the actor system at `pekko.connectors.jakartams.connection-retry`.
   *
   * @param actorSystem The actor system
   */
  def create(actorSystem: ClassicActorSystemProvider): ConnectionRetrySettings = apply(actorSystem)

}
