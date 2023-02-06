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
import org.apache.pekko.stream.SystemMaterializer;
// #import
import org.apache.pekko.stream.connectors.csv.javadsl.CsvFormatting;
import org.apache.pekko.stream.connectors.csv.javadsl.CsvQuotingStyle;

// #import
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CsvFormattingTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;

  public void documentation() {
    char delimiter = CsvFormatting.COMMA;
    char quoteChar = CsvFormatting.DOUBLE_QUOTE;
    char escapeChar = CsvFormatting.BACKSLASH;
    String endOfLine = CsvFormatting.CR_LF;
    Charset charset = StandardCharsets.UTF_8;
    Optional<ByteString> byteOrderMark = Optional.empty();
    // #flow-type
    Flow<Collection<String>, ByteString, ?> flow1 = CsvFormatting.format();

    Flow<Collection<String>, ByteString, ?> flow2 =
        CsvFormatting.format(
            delimiter,
            quoteChar,
            escapeChar,
            endOfLine,
            CsvQuotingStyle.REQUIRED,
            charset,
            byteOrderMark);
    // #flow-type
  }

  @Test
  public void standardCsvFormatShouldWork()
      throws InterruptedException, ExecutionException, TimeoutException {
    CompletionStage<ByteString> completionStage =
        // #formatting
        Source.single(Arrays.asList("one", "two", "three"))
            .via(CsvFormatting.format())
            .runWith(Sink.head(), system);
    // #formatting
    ByteString result = completionStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertThat(result.utf8String(), equalTo("one,two,three\r\n"));
  }

  @BeforeClass
  public static void setup() throws Exception {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void teardown() throws Exception {
    TestKit.shutdownActorSystem(system);
  }

  @After
  public void checkForStageLeaks() {
    StreamTestKit.assertAllStagesStopped(SystemMaterializer.get(system).materializer());
  }
}
