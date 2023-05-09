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

package org.apache.pekko.stream.connectors.pravega.javadsl;

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.annotation.ApiMayChange;
import org.apache.pekko.stream.connectors.pravega.PravegaEvent;

import org.apache.pekko.stream.connectors.pravega.WriterSettings;
import org.apache.pekko.stream.connectors.pravega.PravegaReaderGroupManager;
import org.apache.pekko.stream.connectors.pravega.*;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.FutureConverters;

import io.pravega.client.ClientConfig;
import io.pravega.client.stream.ReaderGroup;
import java.util.concurrent.CompletionStage;

import org.apache.pekko.stream.connectors.pravega.impl.PravegaFlow;
import org.apache.pekko.stream.connectors.pravega.impl.PravegaSource;

@ApiMayChange
public class Pravega {

  public static PravegaReaderGroupManager readerGroup(String scope, ClientConfig clientConfig) {

    return new PravegaReaderGroupManager(scope, clientConfig);
  }
  /**
   * Messages are read from a Pravega stream.
   *
   * <p>Materialized value is a future which completes to Done as soon as Pravega reader in open.
   */
  public static <V> Source<PravegaEvent<V>, CompletionStage<Done>> source(
      ReaderGroup readerGroup, ReaderSettings<V> readerSettings) {
    return Source.fromGraph(new PravegaSource<>(readerGroup, readerSettings))
        .mapMaterializedValue(FutureConverters::<Done>asJava);
  }

  /** Incoming messages are written to Pravega stream and emitted unchanged. */
  public static <V> Flow<V, V, NotUsed> flow(
      String scope, String streamName, WriterSettings<V> writerSettings) {
    return Flow.fromGraph(new PravegaFlow<>(scope, streamName, writerSettings));
  }
  /** Incoming messages are written to Pravega. */
  public static <V> Sink<V, CompletionStage<Done>> sink(
      String scope, String streamName, WriterSettings<V> writerSettings) {
    return flow(scope, streamName, writerSettings).toMat(Sink.ignore(), Keep.right());
  }
}
