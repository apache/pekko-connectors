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

package docs.javadsl;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.cassandra.CassandraSessionSettings;
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraSession;
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraSessionRegistry;
import org.apache.pekko.stream.connectors.cassandra.scaladsl.CassandraAccess;
import org.apache.pekko.testkit.javadsl.TestKit;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class CassandraTestHelper {
  final ActorSystem system;
  final CassandraSession cassandraSession;
  final CassandraAccess cassandraAccess;
  final String keyspaceName;
  final AtomicInteger tableNumber = new AtomicInteger();

  public CassandraTestHelper(String TEST_NAME) {
    system = ActorSystem.create(TEST_NAME);
    CassandraSessionRegistry sessionRegistry = CassandraSessionRegistry.get(system);
    CassandraSessionSettings sessionSettings =
        CassandraSessionSettings.create("pekko.connectors.cassandra");
    cassandraSession = sessionRegistry.sessionFor(sessionSettings);

    cassandraAccess = new CassandraAccess(cassandraSession.delegate());
    keyspaceName = TEST_NAME + System.nanoTime();
    await(cassandraAccess.createKeyspace(keyspaceName));
  }

  public void shutdown() {
    // `dropKeyspace` uses the system dispatcher through `cassandraSession`,
    // so needs to run before the actor system is shut down
    await(cassandraAccess.dropKeyspace(keyspaceName));
    TestKit.shutdownActorSystem(system);
  }

  public String createTableName() {
    return keyspaceName + "." + "test" + tableNumber.incrementAndGet();
  }

  public static <T> T await(CompletionStage<T> cs)
      throws InterruptedException, ExecutionException, TimeoutException {
    return cs.toCompletableFuture().get(10, TimeUnit.SECONDS);
  }

  public static <T> T await(Future<T> future) {
    int seconds = 20;
    try {
      return Await.result(future, FiniteDuration.create(seconds, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      throw new RuntimeException("timeout " + seconds + "s hit", e);
    }
  }
}
