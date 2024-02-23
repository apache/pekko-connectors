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

package org.apache.pekko.stream.connectors.couchbase.scaladsl

import java.util.concurrent.CompletionStage
import org.apache.pekko
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.annotation.InternalApi
import pekko.discovery.Discovery
import pekko.stream.connectors.couchbase.CouchbaseSessionSetting
import pekko.util.JavaDurationConverters._
import pekko.util.FunctionConverters._
import pekko.util.FutureConverters._
import com.typesafe.config.Config

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.FiniteDuration

/**
 * Utility to delegate Couchbase node address lookup to [[https://pekko.apache.org/docs/pekko/current/discovery/index.html Pekko Discovery]].
 */
sealed class DiscoverySupport private {

  /**
   * Use Pekko Discovery to read the addresses for `serviceName` within `lookupTimeout`.
   */
  private def readNodes(
      serviceName: String,
      lookupTimeout: FiniteDuration)(implicit system: ClassicActorSystemProvider): Future[immutable.Seq[String]] = {
    implicit val ec: ExecutionContext = system.classicSystem.dispatcher
    val discovery = Discovery(system).discovery
    discovery.lookup(serviceName, lookupTimeout).map { resolved =>
      resolved.addresses.map(_.host)
    }
  }

  /**
   * Expect a `service` section in Config and use Pekko Discovery to read the addresses for `name` within `lookup-timeout`.
   */
  private def readNodes(config: Config)(implicit system: ClassicActorSystemProvider): Future[immutable.Seq[String]] =
    if (config.hasPath("service")) {
      val serviceName = config.getString("service.name")
      val lookupTimeout = config.getDuration("service.lookup-timeout").asScala
      readNodes(serviceName, lookupTimeout)
    } else throw new IllegalArgumentException(s"config $config does not contain `service` section")

  /**
   * Expects a `service` section in the given Config and reads the given service name's address
   * to be used as Couchbase `nodes`.
   */
  def nodes(
      config: Config)(
      implicit system: ClassicActorSystemProvider): CouchbaseSessionSetting => Future[CouchbaseSessionSetting] = {
    implicit val ec: ExecutionContext = system.classicSystem.dispatcher
    settings =>
      readNodes(config)
        .map { nodes =>
          settings.withNodes(nodes)
        }
  }

  private[couchbase] def nodes(config: Config,
      system: ActorSystem): CouchbaseSessionSetting => Future[CouchbaseSessionSetting] =
    nodes(config)(system)

  /**
   * Internal API: Java wrapper.
   */
  @InternalApi
  private[couchbase] def getNodes(
      config: Config,
      system: ClassicActorSystemProvider)
      : java.util.function.Function[CouchbaseSessionSetting, CompletionStage[CouchbaseSessionSetting]] =
    nodes(config)(system).andThen(_.asJava).asJava

  /**
   * Expects a `service` section in `pekko.connectors.couchbase.session` and reads the given service name's address
   * to be used as Couchbase `nodes`.
   */
  def nodes()(
      implicit system: ClassicActorSystemProvider): CouchbaseSessionSetting => Future[CouchbaseSessionSetting] =
    nodes(system.classicSystem)

  /**
   * Expects a `service` section in `pekko.connectors.couchbase.session` and reads the given service name's address
   * to be used as Couchbase `nodes`.
   */
  def nodes(system: ActorSystem): CouchbaseSessionSetting => Future[CouchbaseSessionSetting] =
    nodes(system.settings.config.getConfig(CouchbaseSessionSetting.configPath))(system)

}

/**
 * Utility to delegate Couchbase node address lookup to [[https://pekko.apache.org/docs/pekko/current/discovery/index.html Pekko Discovery]].
 */
object DiscoverySupport extends DiscoverySupport {

  /** Internal API */
  @InternalApi val INSTANCE: DiscoverySupport = this
}
