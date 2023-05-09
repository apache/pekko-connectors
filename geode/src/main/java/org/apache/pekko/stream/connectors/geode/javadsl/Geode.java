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

package org.apache.pekko.stream.connectors.geode.javadsl;

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.geode.PekkoPdxSerializer;
import org.apache.pekko.stream.connectors.geode.GeodeSettings;
import org.apache.pekko.stream.connectors.geode.RegionSettings;
import org.apache.pekko.stream.connectors.geode.impl.GeodeCache;

import org.apache.pekko.stream.connectors.geode.impl.stage.GeodeFiniteSourceStage;
import org.apache.pekko.stream.connectors.geode.impl.stage.GeodeFlowStage;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.FutureConverters;
import org.apache.geode.cache.client.ClientCacheFactory;

import java.util.concurrent.CompletionStage;

/** Java API: Geode client without server event subscription. */
public class Geode extends GeodeCache {

  final GeodeSettings geodeSettings;

  public Geode(GeodeSettings settings) {
    super(settings);
    this.geodeSettings = settings;
  }

  @Override
  public ClientCacheFactory configure(ClientCacheFactory factory) {
    return factory.addPoolLocator(geodeSettings.hostname(), geodeSettings.port());
  }

  public <V> Source<V, CompletionStage<Done>> query(String query, PekkoPdxSerializer<V> serializer) {

    registerPDXSerializer(serializer, serializer.clazz());
    return Source.fromGraph(new GeodeFiniteSourceStage<V>(cache(), query))
        .mapMaterializedValue(FutureConverters::<Done>asJava);
  }

  public <K, V> Flow<V, V, NotUsed> flow(
          RegionSettings<K, V> regionSettings, PekkoPdxSerializer<V> serializer) {

    registerPDXSerializer(serializer, serializer.clazz());

    return Flow.fromGraph(new GeodeFlowStage<K, V>(cache(), regionSettings));
  }

  public <K, V> Sink<V, CompletionStage<Done>> sink(
          RegionSettings<K, V> regionSettings, PekkoPdxSerializer<V> serializer) {
    return flow(regionSettings, serializer).toMat(Sink.ignore(), Keep.right());
  }

  public void close() {
    close(false);
  }
}
