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
import org.apache.pekko.stream.SystemMaterializer;
import org.apache.pekko.stream.connectors.csv.MalformedCsvException;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.*;

// #import
import org.apache.pekko.stream.connectors.csv.javadsl.CsvParsing;

// #import
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class CsvParsingTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;

  public void documentation() {
    byte delimiter = CsvParsing.COMMA;
    byte quoteChar = CsvParsing.DOUBLE_QUOTE;
    byte escapeChar = CsvParsing.BACKSLASH;
    // #flow-type
    Flow<ByteString, Collection<ByteString>, NotUsed> flow =
        CsvParsing.lineScanner(delimiter, quoteChar, escapeChar);
    // #flow-type
  }

  @Test
  public void lineParserShouldParseOneLine() throws Exception {
    CompletionStage<Collection<ByteString>> completionStage =
        // #line-scanner
        Source.single(ByteString.fromString("eins,zwei,drei\n"))
            .via(CsvParsing.lineScanner())
            .runWith(Sink.head(), system);
    // #line-scanner
    Collection<ByteString> list = completionStage.toCompletableFuture().get(5, TimeUnit.SECONDS);
    String[] res = list.stream().map(ByteString::utf8String).toArray(String[]::new);
    assertThat(res[0], equalTo("eins"));
    assertThat(res[1], equalTo("zwei"));
    assertThat(res[2], equalTo("drei"));
  }

  @Test
  public void lineParserShouldParseOneLineAsString() throws Exception {
    CompletionStage<List<String>> completionStage =
        // #line-scanner-string
        Source.single(ByteString.fromString("eins,zwei,drei\n"))
            .via(CsvParsing.lineScanner())
            .map(line -> line.stream().map(ByteString::utf8String).collect(Collectors.toList()))
            .runWith(Sink.head(), system);
    // #line-scanner-string
    List<String> res = completionStage.toCompletableFuture().get(5, TimeUnit.SECONDS);
    assertThat(res.get(0), equalTo("eins"));
    assertThat(res.get(1), equalTo("zwei"));
    assertThat(res.get(2), equalTo("drei"));
  }

  @Test
  public void illegalFormatShouldThrow() throws Exception {
    CompletionStage<List<Collection<ByteString>>> completionStage =
        Source.single(ByteString.fromString("eins,zwei,drei\na,b,\\\"c"))
            .via(CsvParsing.lineScanner())
            .runWith(Sink.seq(), system);
    try {
      completionStage.toCompletableFuture().get(5, TimeUnit.SECONDS);
      fail("Should throw MalformedCsvException");
    } catch (ExecutionException expected) {
      Throwable cause = expected.getCause();
      assertThat(cause, is(instanceOf(MalformedCsvException.class)));
      MalformedCsvException csvException = (MalformedCsvException) cause;
      assertThat(csvException.getLineNo(), equalTo(2L));
      assertThat(csvException.getBytePos(), equalTo(5));
    }
  }

  @Test
  public void escapeWithoutEscapedByteShouldProduceEscape() throws Exception {
    CompletionStage<List<String>> completionStage =
        Source.single(ByteString.fromString("a,b,\\c"))
            .via(CsvParsing.lineScanner())
            .map(line -> line.stream().map(ByteString::utf8String).collect(Collectors.toList()))
            .runWith(Sink.head(), system);
    List<String> res = completionStage.toCompletableFuture().get(5, TimeUnit.SECONDS);
    assertThat(res.get(0), equalTo("a"));
    assertThat(res.get(1), equalTo("b"));
    assertThat(res.get(2), equalTo("\\c"));
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
