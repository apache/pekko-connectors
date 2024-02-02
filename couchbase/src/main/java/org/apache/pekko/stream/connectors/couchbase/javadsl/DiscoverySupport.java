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

package org.apache.pekko.stream.connectors.couchbase.javadsl;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.stream.connectors.couchbase.CouchbaseSessionSettings;
import com.typesafe.config.Config;

import java.util.concurrent.CompletionStage;

/**
 * Utility to delegate Couchbase node address lookup to
 * [[https://pekko.apache.org/docs/pekko/current/discovery/index.html Pekko Discovery]].
 */
public final class DiscoverySupport {

  private static final org.apache.pekko.stream.connectors.couchbase.scaladsl.DiscoverySupport SUPPORT =
      org.apache.pekko.stream.connectors.couchbase.scaladsl.DiscoverySupport.INSTANCE();

  /**
   * Expects a `service` section in the given Config and reads the given service name's address to
   * be used as Couchbase `nodes`.
   */
  public static java.util.function.Function<
          CouchbaseSessionSettings, CompletionStage<CouchbaseSessionSettings>>
      getNodes(Config config, ActorSystem system) {
    return SUPPORT.getNodes(config, system);
  }

  /**
   * Expects a `service` section in the given Config and reads the given service name's address to
   * be used as Couchbase `nodes`.
   */
  public static java.util.function.Function<
          CouchbaseSessionSettings, CompletionStage<CouchbaseSessionSettings>>
      getNodes(Config config, ClassicActorSystemProvider system) {
    return getNodes(config, system.classicSystem());
  }

  /**
   * Expects a `service` section in the given Config and reads the given service name's address to
   * be used as Couchbase `nodes`.
   */
  public static java.util.function.Function<
          CouchbaseSessionSettings, CompletionStage<CouchbaseSessionSettings>>
      getNodes(ActorSystem system) {
    return SUPPORT.getNodes(
        system.settings().config().getConfig(CouchbaseSessionSettings.configPath()), system);
  }

  /**
   * Expects a `service` section in the given Config and reads the given service name's address to
   * be used as Couchbase `nodes`.
   */
  public static java.util.function.Function<
          CouchbaseSessionSettings, CompletionStage<CouchbaseSessionSettings>>
      getNodes(ClassicActorSystemProvider system) {
    return getNodes(system.classicSystem());
  }

  private DiscoverySupport() {}
}
