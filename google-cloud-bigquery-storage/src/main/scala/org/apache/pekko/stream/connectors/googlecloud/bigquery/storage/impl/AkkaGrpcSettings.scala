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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.impl

import org.apache.pekko
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.InternalApi
import pekko.grpc.GrpcClientSettings
import pekko.stream.connectors.google.RequestSettings
import pekko.stream.connectors.google.auth.Credentials
import pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryStorageSettings
import com.typesafe.config.{ Config, ConfigFactory }
import io.grpc.auth.MoreCallCredentials

import java.util.concurrent.Executor

/**
 * Internal API
 */
@InternalApi private[bigquery] object PekkoGrpcSettings {

  def fromBigQuerySettings(
      config: BigQueryStorageSettings)(implicit system: ClassicActorSystemProvider): GrpcClientSettings = {
    val sslConfig = config.rootCa.fold("") { rootCa =>
      s"""
      |ssl-config {
      |  disabledKeyAlgorithms = []
      |  trustManager = {
      |    stores = [
      |      { type = "PEM", path = "$rootCa", classpath = true }
      |    ]
      |  }
      |}""".stripMargin
    }

    val pekkoGrpcConfig = s"""
      |host = "${config.host}"
      |port = ${config.port}
      |
      |$sslConfig
      |""".stripMargin

    val settings =
      GrpcClientSettings.fromConfig(
        ConfigFactory
          .parseString(pekkoGrpcConfig)
          .withFallback(system.classicSystem.settings.config.getConfig("pekko.grpc.client.\"*\"")))

    val setTls = (settings: GrpcClientSettings) =>
      config.rootCa
        .fold(settings.withTls(false))(_ => settings.withTls(true))

    val setCallCredentials = (settings: GrpcClientSettings) => {
      implicit val config = system.classicSystem.settings.config
      val executor: Executor = system.classicSystem.dispatcher
      settings.withCallCredentials(MoreCallCredentials.from(credentials().asGoogle(executor, requestSettings())))
    }

    Seq(setTls, setCallCredentials).foldLeft(settings) {
      case (s, f) => f(s)
    }
  }

  def credentials()(implicit system: ClassicActorSystemProvider, config: Config): Credentials = {
    val credentialsConfig = config.getConfig("pekko.connectors.google.credentials")
    Credentials(credentialsConfig)
  }

  def requestSettings()(implicit system: ClassicActorSystemProvider, config: Config): RequestSettings = {
    val alpakkaConfig = config.getConfig("pekko.connectors.google")
    RequestSettings(alpakkaConfig)
  }

}
