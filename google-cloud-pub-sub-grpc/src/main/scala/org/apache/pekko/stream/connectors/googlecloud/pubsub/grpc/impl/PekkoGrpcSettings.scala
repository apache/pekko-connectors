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
import pekko.stream.connectors.google.auth.{ Credentials, NoCredentials }
import pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings
import com.typesafe.config.ConfigFactory
import io.grpc.auth.MoreCallCredentials

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

    googleSettings.credentials match {
      case _: NoCredentials   => settings // no credentials (e.g. emulator)
      case creds: Credentials =>
        val credentials = creds.asGoogle(sys.dispatcher, googleSettings.requestSettings)
        settings.withCallCredentials(MoreCallCredentials.from(credentials))
    }
  }
}
