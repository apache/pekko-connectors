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

/*
 * Start package with 'docs' prefix when testing APIs as a user.
 * This prevents any visibility issues that may be hidden.
 */
package docs.javadsl;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.reference.*;
import org.apache.pekko.stream.connectors.reference.javadsl.Reference;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

/** Append "Test" to every Java test suite. */
@ExtendWith(LogCapturingExtension.class)
public class ReferenceTest {

  static ActorSystem system;

  static final String clientId = "test-client-id";

  /** Called before test suite. */
  @BeforeAll
  public static void setUpBeforeClass() {
    system = ActorSystem.create("ReferenceTest");
  }

  /** Called before every test. */
  @BeforeEach
  public void setUp() {}

  @Test
  public void compileSettings() {
    final Authentication.Provided providedAuth =
        Authentication.createProvided().withVerifierPredicate(c -> true);

    final Authentication.None noAuth = Authentication.createNone();

    final SourceSettings settings = SourceSettings.create(clientId);

    settings.withAuthentication(providedAuth);
    settings.withAuthentication(noAuth);
  }

  @Test
  public void compileSource() {
    // #source
    final SourceSettings settings = SourceSettings.create(clientId);

    final Source<ReferenceReadResult, CompletionStage<Done>> source = Reference.source(settings);
    // #source
  }

  @Test
  public void compileFlow() {
    // #flow
    final Flow<ReferenceWriteMessage, ReferenceWriteResult, NotUsed> flow = Reference.flow();
    // #flow

    final Executor ex = Executors.newCachedThreadPool();
    final Flow<ReferenceWriteMessage, ReferenceWriteResult, NotUsed> flow2 =
        Reference.flowAsyncMapped(ex);
  }

  @Test
  public void runSource() throws Exception {
    final Source<ReferenceReadResult, CompletionStage<Done>> source =
        Reference.source(SourceSettings.create(clientId));

    final CompletionStage<ReferenceReadResult> stage = source.runWith(Sink.head(), system);
    final ReferenceReadResult msg = stage.toCompletableFuture().get(5, TimeUnit.SECONDS);

    Assertions.assertEquals(List.of(ByteString.fromString("one")), msg.getData());

    final OptionalInt expected = OptionalInt.of(100);
    Assertions.assertEquals(expected, msg.getBytesRead());

    Assertions.assertEquals(Optional.empty(), msg.getBytesReadFailure());
  }

  @Test
  public void runFlow() throws Exception {
    final Flow<ReferenceWriteMessage, ReferenceWriteResult, NotUsed> flow = Reference.flow();

    Map<String, Long> metrics =
        new HashMap<String, Long>() {
          {
            put("rps", 20L);
            put("rpm", Long.valueOf(30L));
          }
        };

    final Source<ReferenceWriteMessage, NotUsed> source =
        Source.from(
            List.of(
                ReferenceWriteMessage.create()
                    .withData(List.of(ByteString.fromString("one")))
                    .withMetrics(metrics),
                ReferenceWriteMessage.create()
                    .withData(
                        List.of(
                            ByteString.fromString("two"),
                            ByteString.fromString("three"),
                            ByteString.fromString("four")))));

    final CompletionStage<List<ReferenceWriteResult>> stage =
        source.via(flow).runWith(Sink.seq(), system);
    final List<ReferenceWriteResult> result = stage.toCompletableFuture().get(5, TimeUnit.SECONDS);

    final List<ByteString> bytes =
        result.stream().flatMap(m -> m.getMessage().getData().stream()).toList();

    Assertions.assertEquals(
        List.of(
            ByteString.fromString("one"),
            ByteString.fromString("two"),
            ByteString.fromString("three"),
            ByteString.fromString("four")),
        bytes);

    final long actual = result.stream().findFirst().get().getMetrics().get("total");
    Assertions.assertEquals(50L, actual);
  }

  @Test
  public void resolveResourceFromApplicationConfig() throws Exception {
    final List<ReferenceWriteResult> result =
        Source.single(
                ReferenceWriteMessage.create().withData(List.of(ByteString.fromString("one"))))
            .via(Reference.flowWithResource())
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    Assertions.assertEquals(
        List.of("one default msg"),
        result.stream()
            .flatMap(m -> m.getMessage().getData().stream())
            .map(ByteString::utf8String)
            .toList());
  }

  @Test
  public void useResourceFromAttributes() throws Exception {
    final List<ReferenceWriteResult> result =
        Source.single(
                ReferenceWriteMessage.create().withData(List.of(ByteString.fromString("one"))))
            .via(
                Reference.flowWithResource()
                    .withAttributes(
                        ReferenceAttributes.resource(
                            Resource.create(ResourceSettings.create("attributes msg")))))
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    Assertions.assertEquals(
        List.of("one attributes msg"),
        result.stream()
            .flatMap(m -> m.getMessage().getData().stream())
            .map(ByteString::utf8String)
            .toList());
  }

  /** Called after every test. */
  @AfterEach
  public void tearDown() {}

  /** Called after test suite. */
  @AfterAll
  public static void tearDownAfterClass() {
    TestKit.shutdownActorSystem(system);
  }
}
