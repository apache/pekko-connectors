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

package docs.javadsl;

import org.apache.pekko.actor.ActorSystem;
// #registry
import org.apache.pekko.stream.connectors.couchbase.CouchbaseSessionRegistry;
import org.apache.pekko.stream.connectors.couchbase.CouchbaseSessionSettings;
import org.apache.pekko.stream.connectors.couchbase.javadsl.DiscoverySupport;
import org.apache.pekko.stream.connectors.couchbase.javadsl.CouchbaseSession;
// #registry
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoveryTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem actorSystem;
  private static final String bucketName = "pekko";

  @BeforeClass
  public static void beforeAll() {
    Config config = ConfigFactory.parseResources("discovery.conf");
    actorSystem = ActorSystem.create("DiscoveryTest", config);
  }

  @AfterClass
  public static void afterAll() {
    TestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void configDiscovery() throws Exception {
    // #registry

    CouchbaseSessionRegistry registry = CouchbaseSessionRegistry.get(actorSystem);

    CouchbaseSessionSettings sessionSettings =
        CouchbaseSessionSettings.create(actorSystem)
            .withEnrichAsyncCs(DiscoverySupport.getNodes(actorSystem));
    CompletionStage<CouchbaseSession> session = registry.getSessionFor(sessionSettings);
    // #registry
    try {
      CouchbaseSession couchbaseSession = session.toCompletableFuture().get(5, TimeUnit.SECONDS);
    } catch (java.util.concurrent.ExecutionException e) {
      assertThat(
          e.getCause(),
          is(instanceOf(com.couchbase.client.core.error.ConfigException.class)));
    }
  }
}
