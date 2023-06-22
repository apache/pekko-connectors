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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.impl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.annotation.InternalApi
import pekko.grpc.GrpcClientSettings
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings
import com.typesafe.config.ConfigFactory
import io.grpc.auth.MoreCallCredentials

import scala.annotation.nowarn

/**
 * Internal API
 */
@InternalApi private[grpc] object PekkoGrpcSettings {
  def fromPubSubSettings(config: PubSubSettings,
      googleSettings: GoogleSettings)(implicit sys: ActorSystem): GrpcClientSettings = {
    val pekkoGrpcConfig = s"""
      |host = "${config.host}"
      |port = ${config.port}
      |use-tls = ${config.useTls}
      |trusted = "${config.rootCa.getOrElse("")}"
      |""".stripMargin

    val settings = GrpcClientSettings.fromConfig(
      ConfigFactory
        .parseString(pekkoGrpcConfig)
        .withFallback(sys.settings.config.getConfig("pekko.grpc.client.\"*\"")))

    (config.callCredentials: @nowarn("msg=deprecated")) match {
      case None => settings
      case Some(DeprecatedCredentials(_)) => // Deprecated credentials were loaded from config so override them
        sys.log.warning(
          "Config path pekko.connectors.google.cloud.pubsub.grpc.callCredentials is deprecated, use pekko.connectors.google.credentials")
        val credentials = googleSettings.credentials.asGoogle(sys.dispatcher, googleSettings.requestSettings)
        settings.withCallCredentials(MoreCallCredentials.from(credentials))
      case Some(creds) => settings.withCallCredentials(creds)
    }
  }
}
