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

package org.apache.pekko.stream.connectors.cassandra

import org.apache.pekko
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider, ExtendedActorSystem }
import pekko.util.FutureConverters._
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

/**
 * The implementation of the `SessionProvider` is used for creating the
 * Cassandra Session. By default the [[DefaultSessionProvider]] is building
 * the Cluster from configuration properties but it is possible to
 * replace the implementation of the SessionProvider to reuse another
 * session or override the Cluster builder with other settings.
 *
 * The implementation is defined in configuration `session-provider` property.
 * It may optionally have a constructor with an ActorSystem and Config parameter.
 * The config parameter is the config section of the plugin.
 */
trait CqlSessionProvider {
  def connect()(implicit ec: ExecutionContext): Future[CqlSession]
}

/**
 * Builds a `CqlSession` from the given `config` via [[DriverConfigLoaderFromConfig]].
 *
 * The configuration for the driver is typically the `datastax-java-driver` section of the ActorSystem's
 * configuration, but it's possible to use other configuration. The configuration path of the
 * driver's configuration can be defined with `datastax-java-driver-config` property in the
 * given `config`.
 */
class DefaultSessionProvider(system: ActorSystem, config: Config) extends CqlSessionProvider {

  /**
   * Check if Pekko Discovery service lookup should be used. It is part of this class so it
   * doesn't trigger the [[PekkoDiscoverySessionProvider]] class to be loaded.
   */
  private def usePekkoDiscovery(config: Config): Boolean = config.getString("service-discovery.name").nonEmpty

  override def connect()(implicit ec: ExecutionContext): Future[CqlSession] = {
    if (usePekkoDiscovery(config)) {
      PekkoDiscoverySessionProvider.connect(system, config)
    } else {
      val driverConfig = CqlSessionProvider.driverConfig(system, config)
      val driverConfigLoader = DriverConfigLoaderFromConfig.fromConfig(driverConfig)
      CqlSession.builder().withConfigLoader(driverConfigLoader).buildAsync().asScala
    }
  }
}

object CqlSessionProvider {

  /**
   * Create a `SessionProvider` from configuration.
   * The `session-provider` config property defines the fully qualified
   * class name of the SessionProvider implementation class. It may optionally
   * have a constructor with an `ActorSystem` and `Config` parameter.
   */
  def apply(system: ExtendedActorSystem, config: Config): CqlSessionProvider = {
    val className = config.getString("session-provider")
    val dynamicAccess = system.dynamicAccess
    val clazz = dynamicAccess.getClassFor[CqlSessionProvider](className).get
    def instantiate(args: immutable.Seq[(Class[_], AnyRef)]) =
      dynamicAccess.createInstanceFor[CqlSessionProvider](clazz, args)

    val params = List((classOf[ActorSystem], system), (classOf[Config], config))
    instantiate(params)
      .recoverWith {
        case _: NoSuchMethodException => instantiate(params.take(1))
      }
      .recoverWith { case _: NoSuchMethodException => instantiate(Nil) }
      .recoverWith {
        case ex: Exception =>
          Failure(
            new IllegalArgumentException(
              s"Unable to create SessionProvider instance for class [$className], " +
              "tried constructor with ActorSystem, Config, and only ActorSystem, and no parameters",
              ex))
      }
      .get
  }

  /**
   * Create a `SessionProvider` from configuration.
   * The `session-provider` config property defines the fully qualified
   * class name of the SessionProvider implementation class. It may optionally
   * have a constructor with an `ActorSystem` and `Config` parameter.
   */
  def apply(system: ClassicActorSystemProvider, config: Config): CqlSessionProvider =
    apply(system.classicSystem.asInstanceOf[ExtendedActorSystem], config)

  /**
   * The `Config` for the `datastax-java-driver`. The configuration path of the
   * driver's configuration can be defined with `datastax-java-driver-config` property in the
   * given `config`. `datastax-java-driver` configuration section is also used as fallback.
   */
  def driverConfig(system: ActorSystem, config: Config): Config = {
    val driverConfigPath = config.getString("datastax-java-driver-config")
    system.classicSystem.settings.config.getConfig(driverConfigPath).withFallback {
      if (driverConfigPath == "datastax-java-driver") ConfigFactory.empty()
      else system.classicSystem.settings.config.getConfig("datastax-java-driver")
    }
  }

  /**
   * The `Config` for the `datastax-java-driver`. The configuration path of the
   * driver's configuration can be defined with `datastax-java-driver-config` property in the
   * given `config`. `datastax-java-driver` configuration section is also used as fallback.
   */
  def driverConfig(system: ClassicActorSystemProvider, config: Config): Config =
    driverConfig(system.classicSystem, config)
}
