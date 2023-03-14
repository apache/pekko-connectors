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

package org.apache.pekko.stream.connectors.orientdb

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

final class OrientDbSourceSettings private (
    val oDatabasePool: com.orientechnologies.orient.core.db.OPartitionedDatabasePool,
    val skip: Int,
    val limit: Int) {

  def withOrientDBCredentials(
      value: com.orientechnologies.orient.core.db.OPartitionedDatabasePool): OrientDbSourceSettings =
    copy(oDatabasePool = value)
  def withSkip(value: Int): OrientDbSourceSettings = copy(skip = value)
  def withLimit(value: Int): OrientDbSourceSettings = copy(limit = value)

  private def copy(
      oDatabasePool: com.orientechnologies.orient.core.db.OPartitionedDatabasePool = oDatabasePool,
      skip: Int = skip,
      limit: Int = limit): OrientDbSourceSettings = new OrientDbSourceSettings(
    oDatabasePool = oDatabasePool,
    skip = skip,
    limit = limit)

  override def toString =
    "OrientDBSourceSettings(" +
    s"oDatabasePool=$oDatabasePool," +
    s"skip=$skip," +
    s"limit=$limit" +
    ")"
}

object OrientDbSourceSettings {

  /** Scala API */
  def apply(oDatabasePool: OPartitionedDatabasePool): OrientDbSourceSettings = new OrientDbSourceSettings(
    oDatabasePool,
    skip = 0,
    limit = 10)

  /** Java API */
  def create(oDatabasePool: OPartitionedDatabasePool): OrientDbSourceSettings = apply(oDatabasePool)
}
