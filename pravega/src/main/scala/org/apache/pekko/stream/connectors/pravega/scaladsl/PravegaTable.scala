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

package org.apache.pekko.stream.connectors.pravega.scaladsl

import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.stream.scaladsl.{ Flow, Keep, Sink, Source }
import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.stream.connectors.pravega.impl.{ PravegaTableSource, PravegaTableWriteFlow }
import org.apache.pekko.stream.connectors.pravega.{ TableReaderSettings, TableSettings, TableWriterSettings }

import scala.concurrent.Future
import org.apache.pekko.stream.connectors.pravega.impl.PravegaTableReadFlow
import org.apache.pekko.stream.connectors.pravega.TableEntry

@ApiMayChange
object PravegaTable {

  /**
   * Messages are read from a Pravega stream.
   *
   * Materialized value is a [[Future]] which completes to [[Done]] as soon as the Pravega reader is open.
   */
  def source[K, V](scope: String,
      tableName: String,
      tableReaderSettings: TableReaderSettings[K, V]): Source[TableEntry[V], Future[Done]] =
    Source.fromGraph(
      new PravegaTableSource[K, V](
        scope,
        tableName,
        tableReaderSettings))

  /**
   * A flow from key to and Option[value].
   */
  def readFlow[K, V](scope: String,
      tableName: String,
      tableSettings: TableSettings[K, V]): Flow[K, Option[V], NotUsed] =
    Flow.fromGraph(new PravegaTableReadFlow(scope, tableName, tableSettings))

  /**
   * Keys and values are extracted from incoming messages and written to Pravega table.
   * Messages are emitted downstream unchanged.
   */
  def writeFlow[K, V](scope: String,
      tableName: String,
      tableWriterSettings: TableWriterSettings[K, V]): Flow[(K, V), (K, V), NotUsed] =
    Flow.fromGraph(
      new PravegaTableWriteFlow[(K, V), K, V]((o: (K, V)) => o, scope, tableName, tableWriterSettings))

  /**
   * Incoming messages are written to a Pravega table KV.
   */
  def sink[K, V](scope: String,
      tableName: String,
      tableWriterSettings: TableWriterSettings[K, V]): Sink[(K, V), Future[Done]] =
    Flow[(K, V)].via(writeFlow(scope, tableName, tableWriterSettings)).toMat(Sink.ignore)(Keep.right)

}
