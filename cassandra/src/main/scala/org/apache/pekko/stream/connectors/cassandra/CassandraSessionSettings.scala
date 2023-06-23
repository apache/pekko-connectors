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

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.util.FunctionConverters._
import pekko.util.FutureConverters._
import com.datastax.oss.driver.api.core.CqlSession

import scala.concurrent.Future

class CassandraSessionSettings private (val configPath: String,
    _metricsCategory: Option[String] = None,
    val init: Option[CqlSession => Future[Done]] = None) {

  def metricsCategory: String = _metricsCategory.getOrElse(configPath)

  def withMetricCategory(value: String): CassandraSessionSettings =
    copy(metricsCategory = Option(value))

  /**
   * Java API:
   *
   * The `init` function will be performed once when the session is created, i.e.
   * if `CassandraSessionRegistry.sessionFor` is called from multiple places with different `init` it will
   * only execute the first.
   */
  def withInit(value: java.util.function.Function[CqlSession, CompletionStage[Done]]): CassandraSessionSettings =
    copy(init = Some(value.asScala.andThen(_.asScala)))

  /**
   * The `init` function will be performed once when the session is created, i.e.
   * if `CassandraSessionRegistry.sessionFor` is called from multiple places with different `init` it will
   * only execute the first.
   */
  def withInit(value: CqlSession => Future[Done]): CassandraSessionSettings = copy(init = Some(value))

  private def copy(configPath: String = configPath,
      metricsCategory: Option[String] = _metricsCategory,
      init: Option[CqlSession => Future[Done]] = init) =
    new CassandraSessionSettings(configPath, metricsCategory, init)

  override def toString: String =
    "CassandraSessionSettings(" +
    s"configPath=$configPath," +
    s"metricsCategory=$metricsCategory," +
    s"init=$init)"
}

object CassandraSessionSettings {

  val ConfigPath = "pekko.connectors.cassandra"

  def apply(): CassandraSessionSettings = apply(ConfigPath)

  def apply(configPath: String, init: CqlSession => Future[Done]): CassandraSessionSettings =
    new CassandraSessionSettings(configPath, init = Some(init))

  def apply(configPath: String): CassandraSessionSettings = new CassandraSessionSettings(configPath)

  /** Java API */
  def create(): CassandraSessionSettings = apply(ConfigPath)

  /** Java API */
  def create(configPath: String): CassandraSessionSettings = apply(configPath)
}
