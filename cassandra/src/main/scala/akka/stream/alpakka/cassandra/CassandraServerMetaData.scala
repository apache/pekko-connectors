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

package akka.stream.alpakka.cassandra

final class CassandraServerMetaData(val clusterName: String, val dataCenter: String, val version: String) {

  val isVersion2: Boolean = version.startsWith("2.")

  override def toString: String =
    s"CassandraServerMetaData($clusterName,$dataCenter,$version)"

}
