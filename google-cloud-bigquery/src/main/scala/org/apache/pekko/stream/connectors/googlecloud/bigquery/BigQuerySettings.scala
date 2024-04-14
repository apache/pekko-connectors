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

package org.apache.pekko.stream.connectors.googlecloud.bigquery

import org.apache.pekko
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.InternalApi
import pekko.util.JavaDurationConverters._
import com.typesafe.config.Config

import java.time
import scala.concurrent.duration._

object BigQuerySettings {
  val ConfigPath = "pekko.connectors.google.bigquery"

  /**
   * Reads from the given config.
   */
  def apply(c: Config): BigQuerySettings =
    BigQuerySettings(c.getDuration("load-job-per-table-quota").asScala)

  /**
   * Java API: Reads from the given config.
   */
  def create(c: Config): BigQuerySettings = apply(c)

  /**
   * Scala API: Creates [[BigQuerySettings]] from the [[com.typesafe.config.Config Config]] attached to an actor system.
   */
  def apply()(implicit system: ClassicActorSystemProvider, dummy: DummyImplicit): BigQuerySettings = apply(system)

  /**
   * Scala API: Creates [[BigQuerySettings]] from the [[com.typesafe.config.Config Config]] attached to an [[pekko.actor.ActorSystem]].
   */
  def apply(system: ClassicActorSystemProvider): BigQuerySettings = BigQueryExt(system.classicSystem).settings

  implicit def settings(implicit system: ClassicActorSystemProvider): BigQuerySettings = apply(system)

  /**
   * Java API: Creates [[BigQuerySettings]] from the [[com.typesafe.config.Config Config]] attached to an actor system.
   */
  def create(system: ClassicActorSystemProvider): BigQuerySettings = apply(system)

  /**
   * Java API
   */
  def create(loadJobPerTableQuota: time.Duration): BigQuerySettings = BigQuerySettings(loadJobPerTableQuota.asScala)

}

final case class BigQuerySettings @InternalApi private (loadJobPerTableQuota: FiniteDuration) {
  def getLoadJobPerTableQuota: time.Duration = loadJobPerTableQuota.asJava
  def withLoadJobPerTableQuota(loadJobPerTableQuota: FiniteDuration): BigQuerySettings =
    copy(loadJobPerTableQuota = loadJobPerTableQuota)
  def withLoadJobPerTableQuota(loadJobPerTableQuota: time.Duration): BigQuerySettings =
    copy(loadJobPerTableQuota = loadJobPerTableQuota.asScala)
}
