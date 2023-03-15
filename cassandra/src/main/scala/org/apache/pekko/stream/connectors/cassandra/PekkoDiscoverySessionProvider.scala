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

package org.apache.pekko.stream.connectors.cassandra

import org.apache.pekko
import pekko.ConfigurationException
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.discovery.Discovery
import pekko.util.JavaDurationConverters._
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.immutable
import scala.compat.java8.FutureConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * [[https://doc.akka.io/docs/akka/current/discovery/index.html Pekko Discovery]]
 * is enabled by setting the `service-discovery.name` in the given `CassandraSession` config.
 *
 * Pekko Discovery overwrites the basic.contact-points` from the configuration with addresses
 * provided by the configured Pekko Discovery mechanism.
 *
 * Example using config-based Pekko Discovery:
 * {{{
 * pekko {
 *   discovery.method = config
 * }
 * pekko.discovery.config.services = {
 *   cassandra-service = {
 *     endpoints = [
 *       {
 *         host = "127.0.0.1"
 *         port = 9042
 *       },
 *       {
 *         host = "127.0.0.2"
 *         port = 9042
 *       }
 *     ]
 *   }
 * }
 * pekko.connectors.cassandra {
 *   service-discovery.name ="cassandra-service"
 * }
 * }}}
 *
 * Look up this `CassandraSession` with
 * {{{
 * CassandraSessionRegistry
 *   .get(system)
 *   .sessionFor(CassandraSessionSettings.create())
 * }}}
 */
private[cassandra] object PekkoDiscoverySessionProvider {

  def connect(system: ActorSystem, config: Config)(implicit ec: ExecutionContext): Future[CqlSession] = {
    readNodes(config)(system, ec).flatMap { contactPoints =>
      val driverConfigWithContactPoints = ConfigFactory.parseString(s"""
        basic.contact-points = [${contactPoints.mkString("\"", "\", \"", "\"")}]
        """).withFallback(CqlSessionProvider.driverConfig(system, config))
      val driverConfigLoader = DriverConfigLoaderFromConfig.fromConfig(driverConfigWithContactPoints)
      CqlSession.builder().withConfigLoader(driverConfigLoader).buildAsync().toScala
    }
  }

  def connect(system: ClassicActorSystemProvider, config: Config)(implicit ec: ExecutionContext): Future[CqlSession] =
    connect(system.classicSystem, config)

  /**
   * Expect a `service` section in Config and use Pekko Discovery to read the addresses for `name` within `lookup-timeout`.
   */
  private def readNodes(config: Config)(implicit system: ActorSystem,
      ec: ExecutionContext): Future[immutable.Seq[String]] = {
    val serviceConfig = config.getConfig("service-discovery")
    val serviceName = serviceConfig.getString("name")
    val lookupTimeout = serviceConfig.getDuration("lookup-timeout").asScala
    readNodes(serviceName, lookupTimeout)
  }

  /**
   * Use Pekko Discovery to read the addresses for `serviceName` within `lookupTimeout`.
   */
  private def readNodes(
      serviceName: String,
      lookupTimeout: FiniteDuration)(
      implicit system: ActorSystem, ec: ExecutionContext): Future[immutable.Seq[String]] = {
    Discovery(system).discovery.lookup(serviceName, lookupTimeout).map { resolved =>
      resolved.addresses.map { target =>
        target.host + ":" + target.port.getOrElse {
          throw new ConfigurationException(
            s"Pekko Discovery for Cassandra service [$serviceName] must provide a port for [${target.host}]")
        }
      }
    }
  }

}
