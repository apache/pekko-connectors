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

package org.apache.pekko.stream.connectors.pravega.javadsl;

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.annotation.ApiMayChange;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.pravega.*;
import org.apache.pekko.stream.connectors.pravega.impl.PravegaTableSource;
import org.apache.pekko.stream.connectors.pravega.impl.PravegaTableWriteFlow;
import org.apache.pekko.stream.connectors.pravega.impl.PravegaTableReadFlow;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.FutureConverters;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.nio.ByteBuffer;

import io.pravega.client.tables.TableKey;

import org.apache.pekko.util.OptionConverters;

import scala.Option;

@ApiMayChange
public class PravegaTable {

  /**
   * Keys, values and key families are extracted from incoming messages and written to Pravega
   * table. Messages are emitted downstream unchanged.
   */
  public static <K, V> Flow<Pair<K, V>, Pair<K, V>, NotUsed> writeFlow(
      String scope, String tableName, TableWriterSettings<K, V> tableWriterSettings) {
    return Flow.fromGraph(
        new PravegaTableWriteFlow<Pair<K, V>, K, V>(
            Pair::toScala, scope, tableName, tableWriterSettings));
  }

  /** Incoming messages are written to a Pravega table KV. */
  public static <K, V> Sink<Pair<K, V>, CompletionStage<Done>> sink(
      String scope, String tableName, TableWriterSettings<K, V> tableWriterSettings) {
    return writeFlow(scope, tableName, tableWriterSettings).toMat(Sink.ignore(), Keep.right());
  }

  /**
   * Messages are read from a Pravega table.
   *
   * <p>Materialized value is a [[Future]] which completes to [[Done]] as soon as the Pravega reader
   * is open.
   */
  public static <K, V> Source<TableEntry<V>, CompletionStage<Done>> source(
      String scope, String tableName, TableReaderSettings<K, V> tableReaderSettings) {
    return Source.fromGraph(new PravegaTableSource<K, V>(scope, tableName, tableReaderSettings))
        .mapMaterializedValue(FutureConverters::asJava);
  }
  /** A flow from key to and Option[value]. */
  public static <K, V> Flow<K, Optional<V>, NotUsed> readFlow(
      String scope, String tableName, TableSettings<K, V> tableSettings) {
    return Flow.fromGraph(new PravegaTableReadFlow<K, V>(scope, tableName, tableSettings))
        .map(o -> OptionConverters.toJava(o));
  }
}
