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

package org.apache.pekko.stream.connectors.orientdb

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

final class OrientDbWriteSettings private (
    val oDatabasePool: com.orientechnologies.orient.core.db.OPartitionedDatabasePool) {

  def withOrientDBCredentials(
      value: com.orientechnologies.orient.core.db.OPartitionedDatabasePool): OrientDbWriteSettings =
    copy(oDatabasePool = value)

  private def copy(
      oDatabasePool: com.orientechnologies.orient.core.db.OPartitionedDatabasePool): OrientDbWriteSettings =
    new OrientDbWriteSettings(
      oDatabasePool = oDatabasePool)

  override def toString =
    "OrientDBUpdateSettings(" +
    s"oDatabasePool=$oDatabasePool" +
    ")"
}

object OrientDbWriteSettings {

  /** Scala API */
  def apply(oDatabasePool: OPartitionedDatabasePool): OrientDbWriteSettings =
    new OrientDbWriteSettings(
      oDatabasePool: OPartitionedDatabasePool)

  /** Java API */
  def create(oDatabasePool: OPartitionedDatabasePool): OrientDbWriteSettings = apply(oDatabasePool)
}
