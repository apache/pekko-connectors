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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc

import org.apache.pekko
import pekko.actor.ClassicActorSystemProvider
import com.typesafe.config.Config

/**
 * Connection settings used to establish Pub/Sub connection.
 */
final class PubSubSettings private (
    val host: String,
    val port: Int,
    val useTls: Boolean,
    val rootCa: Option[String]) {

  /**
   * Endpoint hostname where the gRPC connection is made.
   */
  def withHost(host: String): PubSubSettings = copy(host = host)

  /**
   * Endpoint port where the gRPC connection is made.
   */
  def withPort(port: Int): PubSubSettings = copy(port = port)

  /**
   * A filename on the classpath which contains the root certificate authority
   * that is going to be used to verify certificate presented by the gRPC endpoint.
   */
  def withRootCa(rootCa: String): PubSubSettings =
    copy(rootCa = Some(rootCa))

  private def copy(host: String = host,
      port: Int = port,
      useTls: Boolean = useTls,
      rootCa: Option[String] = rootCa) =
    new PubSubSettings(host, port, useTls, rootCa)
}

object PubSubSettings {

  /**
   * Create settings for unsecure (no tls), unauthenticated (no root ca) endpoint.
   */
  def apply(host: String, port: Int): PubSubSettings =
    new PubSubSettings(host, port, false, None)

  /**
   * Create settings from config instance.
   */
  def apply(config: Config): PubSubSettings =
    new PubSubSettings(
      config.getString("host"),
      config.getInt("port"),
      config.getBoolean("use-tls"),
      Some(config.getString("rootCa")).filter(_ != "none"))

  /**
   * Create settings from the new actor API's ActorSystem config.
   */
  def apply(system: ClassicActorSystemProvider): PubSubSettings = apply(system.classicSystem)

  /**
   * Create settings from a classic ActorSystem's config.
   */
  def apply(system: pekko.actor.ActorSystem): PubSubSettings =
    PubSubSettings(system.settings.config.getConfig("pekko.connectors.google.cloud.pubsub.grpc"))

  /**
   * Java API
   *
   * Create settings for unsecure (no tls), unauthenticated (no root ca) endpoint.
   */
  def create(host: String, port: Int): PubSubSettings =
    PubSubSettings(host, port)

  /**
   * Java API
   *
   * Create settings from config instance.
   */
  def create(config: Config): PubSubSettings =
    PubSubSettings(config)

  /**
   * Java API
   *
   * Create settings from ActorSystem's config.
   */
  def create(system: ClassicActorSystemProvider): PubSubSettings = PubSubSettings(system.classicSystem)

  /**
   * Java API
   *
   * Create settings from a classic ActorSystem's config.
   */
  def create(system: pekko.actor.ActorSystem): PubSubSettings =
    PubSubSettings(system)
}
