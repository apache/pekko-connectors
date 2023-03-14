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

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.recordio.javadsl.RecordIOFraming;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RecordIOFramingTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static final ActorSystem system = ActorSystem.create();

  @AfterClass
  public static void afterAll() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void parseStream() throws InterruptedException, ExecutionException, TimeoutException {
    // #run-via-scanner
    String firstRecordData =
        "{\"type\": \"SUBSCRIBED\",\"subscribed\": {\"framework_id\": {\"value\":\"12220-3440-12532-2345\"},\"heartbeat_interval_seconds\":15.0}";
    String secondRecordData = "{\"type\":\"HEARTBEAT\"}";

    String firstRecordWithPrefix = "121\n" + firstRecordData;
    String secondRecordWithPrefix = "20\n" + secondRecordData;

    Source<ByteString, NotUsed> basicSource =
        Source.single(ByteString.fromString(firstRecordWithPrefix + secondRecordWithPrefix));

    CompletionStage<List<ByteString>> result =
        basicSource.via(RecordIOFraming.scanner()).runWith(Sink.seq(), system);
    // #run-via-scanner

    // #result
    List<ByteString> byteStrings = result.toCompletableFuture().get(1, TimeUnit.SECONDS);

    assertThat(byteStrings.get(0), is(ByteString.fromString(firstRecordData)));
    assertThat(byteStrings.get(1), is(ByteString.fromString(secondRecordData)));
    // #result
  }
}
