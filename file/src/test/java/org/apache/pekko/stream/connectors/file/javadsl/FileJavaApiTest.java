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

package org.apache.pekko.stream.connectors.file.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.file.TarArchiveMetadata;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exercises the Java API additions of the file connector. */
public class FileJavaApiTest {

  private static ActorSystem system;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void tarArchiveMetadataFilePathPrefix() {
    TarArchiveMetadata metadata = TarArchiveMetadata.create("dir", "file.txt", 1L, Instant.now());
    assertEquals(Optional.of("dir"), metadata.getFilePathPrefix());
    assertEquals(Optional.empty(), TarArchiveMetadata.create("file.txt", 1L).getFilePathPrefix());
  }

  @Test
  public void typedLogRotatorSink() throws Exception {
    AtomicInteger counter = new AtomicInteger();
    List<List<Integer>> rotated = new java.util.concurrent.CopyOnWriteArrayList<>();
    Sink<Integer, CompletionStage<Done>> sink =
        LogRotatorSink.withTypedSinkFactory(
            () ->
                (Integer element) ->
                    element % 2 == 0 ? Optional.of(counter.incrementAndGet()) : Optional.empty(),
            (Integer criterion) ->
                Sink.<Integer>seq()
                    .mapMaterializedValue(
                        values ->
                            values
                                .thenApply(v -> rotated.add(v))
                                .thenApply(x -> Done.getInstance())));

    Source.from(Arrays.asList(0, 1, 2, 3))
        .runWith(sink, system)
        .toCompletableFuture()
        .get(3, TimeUnit.SECONDS);
    assertEquals(Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2, 3)), rotated);
  }

  /** Compile-only check: any OpenOption set can be passed. */
  @SuppressWarnings("unused")
  private static void openOptionsCompile() {
    Set<OpenOption> options = new HashSet<>();
    options.add(StandardOpenOption.APPEND);
    options.add(LinkOption.NOFOLLOW_LINKS);
    Sink<org.apache.pekko.util.ByteString, CompletionStage<Done>> sink =
        LogRotatorSink.createFromFunctionAndOptions(() -> bytes -> Optional.<Path>empty(), options);
    Set<StandardOpenOption> standard = new HashSet<>();
    standard.add(StandardOpenOption.CREATE);
    Sink<org.apache.pekko.util.ByteString, CompletionStage<Done>> sink2 =
        LogRotatorSink.createFromFunctionAndOptions(
            () -> bytes -> Optional.<Path>empty(), standard);
  }
}
