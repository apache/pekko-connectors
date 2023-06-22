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

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.function.Creator;
import org.apache.pekko.japi.function.Function;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.connectors.file.javadsl.LogRotatorSink;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.*;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.*;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class LogRotatorSinkTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;

  @BeforeClass
  public static void beforeAll() throws Exception {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void afterAll() throws Exception {
    TestKit.shutdownActorSystem(system);
  }

  @After
  public void checkForStageLeaks() {
    StreamTestKit.assertAllStagesStopped(Materializer.matFromSystem(system));
  }

  @Test
  public void sizeBased() throws Exception {
    // #size
    Creator<Function<ByteString, Optional<Path>>> sizeBasedTriggerCreator =
        () -> {
          long max = 10 * 1024 * 1024;
          final long[] size = new long[] {max};
          return (element) -> {
            if (size[0] + element.size() > max) {
              Path path = Files.createTempFile("out-", ".log");
              size[0] = element.size();
              return Optional.of(path);
            } else {
              size[0] += element.size();
              return Optional.empty();
            }
          };
        };

    Sink<ByteString, CompletionStage<Done>> sizeRotatorSink =
        LogRotatorSink.createFromFunction(sizeBasedTriggerCreator);
    // #size
    CompletionStage<Done> fileSizeCompletion =
        Source.from(Arrays.asList("test1", "test2", "test3", "test4", "test5", "test6"))
            .map(ByteString::fromString)
            .runWith(sizeRotatorSink, system);

    assertEquals(
        Done.getInstance(), fileSizeCompletion.toCompletableFuture().get(2, TimeUnit.SECONDS));
  }

  @Test
  public void timeBased() throws Exception {
    // #time
    final Path destinationDir = FileSystems.getDefault().getPath("/tmp");
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'stream-'yyyy-MM-dd_HH'.log'");

    Creator<Function<ByteString, Optional<Path>>> timeBasedTriggerCreator =
        () -> {
          final String[] currentFileName = new String[] {null};
          return (element) -> {
            String newName = LocalDateTime.now().format(formatter);
            if (newName.equals(currentFileName[0])) {
              return Optional.empty();
            } else {
              currentFileName[0] = newName;
              return Optional.of(destinationDir.resolve(newName));
            }
          };
        };

    Sink<ByteString, CompletionStage<Done>> timeBasedSink =
        LogRotatorSink.createFromFunction(timeBasedTriggerCreator);
    // #time

    CompletionStage<Done> fileSizeCompletion =
        Source.from(Arrays.asList("test1", "test2", "test3", "test4", "test5", "test6"))
            .map(ByteString::fromString)
            .runWith(timeBasedSink, system);

    assertEquals(
        Done.getInstance(), fileSizeCompletion.toCompletableFuture().get(2, TimeUnit.SECONDS));

    /*
    // #sample
    import org.apache.pekko.stream.connectors.file.javadsl.LogRotatorSink;

    Creator<Function<ByteString, Optional<Path>>> triggerFunctionCreator = ...;

    // #sample
    */
    Creator<Function<ByteString, Optional<Path>>> triggerFunctionCreator = timeBasedTriggerCreator;

    Source<ByteString, NotUsed> source =
        Source.from(Arrays.asList("test1", "test2", "test3", "test4", "test5", "test6"))
            .map(ByteString::fromString);
    // #sample
    CompletionStage<Done> completion =
        Source.from(Arrays.asList("test1", "test2", "test3", "test4", "test5", "test6"))
            .map(ByteString::fromString)
            .runWith(LogRotatorSink.createFromFunction(triggerFunctionCreator), system);

    // GZip compressing the data written
    CompletionStage<Done> compressedCompletion =
        source.runWith(
            LogRotatorSink.withSinkFactory(
                triggerFunctionCreator,
                path ->
                    Flow.of(ByteString.class)
                        .via(Compression.gzip())
                        .toMat(FileIO.toPath(path), Keep.right())),
            system);
    // #sample

    assertEquals(Done.getInstance(), completion.toCompletableFuture().get(2, TimeUnit.SECONDS));
    assertEquals(
        Done.getInstance(), compressedCompletion.toCompletableFuture().get(2, TimeUnit.SECONDS));
  }
}
