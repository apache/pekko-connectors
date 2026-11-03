/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.cassandra.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datastax.oss.driver.api.core.CqlSession;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.cassandra.CassandraSessionSettings;
import org.apache.pekko.stream.connectors.cassandra.CassandraWriteSettings;
import org.apache.pekko.stream.connectors.cassandra.CqlSessionProvider;
import org.junit.jupiter.api.Test;
import scala.concurrent.ExecutionContext;
import scala.jdk.javaapi.FutureConverters;

/** Exercises the Java API of the Cassandra settings without a Cassandra cluster. */
public class CassandraJavaApiTest {

  @Test
  public void sessionSettingsInitFromJavaLambda() {
    CassandraSessionSettings settings =
        CassandraSessionSettings.create()
            .withInitCs(session -> CompletableFuture.completedFuture(Done.getInstance()));
    assertTrue(settings.getInit().isPresent());

    Function<CqlSession, CompletionStage<Done>> init = settings.getInit().get();
    assertEquals(Done.getInstance(), init.apply(null).toCompletableFuture().join());

    assertFalse(CassandraSessionSettings.create().getInit().isPresent());

    CassandraSessionSettings created =
        CassandraSessionSettings.create(
            "custom.path", session -> CompletableFuture.completedFuture(Done.getInstance()));
    assertEquals("custom.path", created.configPath());
    assertTrue(created.getInit().isPresent());
  }

  @Test
  public void writeSettingsMaxBatchWait() {
    CassandraWriteSettings settings =
        CassandraWriteSettings.create().withMaxBatchWait(Duration.ofSeconds(2));
    assertEquals(Duration.ofSeconds(2), settings.getMaxBatchWait());
  }

  @Test
  public void cqlSessionProviderFromJava() throws Exception {
    AtomicReference<Executor> usedExecutor = new AtomicReference<>();
    CqlSessionProvider provider =
        new AbstractCqlSessionProvider() {
          @Override
          public CompletionStage<CqlSession> connect(Executor executor) {
            usedExecutor.set(executor);
            return CompletableFuture.completedFuture(null);
          }
        };

    ExecutionContext ec = ExecutionContext.global();
    assertSame(
        null,
        FutureConverters.asJava(provider.connect(ec))
            .toCompletableFuture()
            .get(3, TimeUnit.SECONDS));
    assertSame(ec, usedExecutor.get());
  }

  /**
   * Compile-only check: a stand-alone session can be created with a java.util.concurrent.Executor.
   */
  @SuppressWarnings("unused")
  private static CassandraSession createSession(
      ActorSystem system, CqlSessionProvider provider, Executor executor) {
    return CassandraSession.create(
        system,
        provider,
        executor,
        system.log(),
        "metrics",
        session -> CompletableFuture.completedFuture(Done.getInstance()),
        () -> {});
  }
}
