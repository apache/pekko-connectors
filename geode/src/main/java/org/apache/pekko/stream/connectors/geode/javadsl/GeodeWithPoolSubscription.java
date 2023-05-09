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
import org.apache.pekko.stream.connectors.geode.PekkoPdxSerializer;
import org.apache.pekko.stream.connectors.geode.GeodeSettings;
import org.apache.pekko.stream.connectors.geode.impl.stage.GeodeContinuousSourceStage;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.FutureConverters;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.query.CqException;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.QueryService;

import java.util.concurrent.CompletionStage;

/** Java API: Geode client with server event subscription. Can build continuous sources. */
public class GeodeWithPoolSubscription extends Geode {

  /**
   * Subscribes to server events.
   *
   * @return ClientCacheFactory with server event subscription.
   */
  public final ClientCacheFactory configure(ClientCacheFactory factory) {
    return super.configure(factory).setPoolSubscriptionEnabled(true);
  }

  public GeodeWithPoolSubscription(GeodeSettings settings) {
    super(settings);
  }

  public <V> Source<V, CompletionStage<Done>> continuousQuery(
          String queryName, String query, PekkoPdxSerializer<V> serializer) {
    registerPDXSerializer(serializer, serializer.clazz());
    return Source.fromGraph(new GeodeContinuousSourceStage<V>(cache(), queryName, query))
        .mapMaterializedValue(FutureConverters::<Done>asJava);
  }

  public boolean closeContinuousQuery(String name) throws CqException {
    QueryService qs = cache().getQueryService();
    CqQuery query = qs.getCq(name);
    if (query == null) return false;
    query.close();
    return true;
  }
}
