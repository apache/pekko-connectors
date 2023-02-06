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
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.stream.connectors.file.TarArchiveMetadata;
import org.apache.pekko.stream.connectors.file.javadsl.Archive;
import org.apache.pekko.stream.connectors.file.javadsl.Directory;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.*;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NestedTarReaderTest {
  private static final Logger logger = LoggerFactory.getLogger(NestedTarReaderTest.class);

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static final String TARGZ_EXT = "tar.gz";
  private static final int MAX_GUNZIP_CHUNK_SIZE = 64000;

  private static ActorSystem system;

  @BeforeClass
  public static void beforeAll() throws Exception {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void afterAll() throws Exception {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void flowShouldCreateZIPArchive() throws Exception {
    Path tempDir = Files.createTempDirectory("alpakka-ftp");
    tempDir.toFile().deleteOnExit();
    Path file = Paths.get("./file/src/test/resources/nested-sample.tar");
    logger.info(
        "extracting {} into {}",
        file.toAbsolutePath().toString(),
        tempDir.toAbsolutePath().toString());
    List<TarArchiveMetadata> metadata =
        process(file, tempDir, system).toCompletableFuture().get(1, TimeUnit.MINUTES);
    List<String> names =
        metadata.stream().map(md -> md.filePathName()).collect(Collectors.toList());
    assertThat(names.size(), is(1281));
  }

  public static CompletionStage<List<TarArchiveMetadata>> process(
      Path filename, Path targetDir, ClassicActorSystemProvider system) {
    return FileIO.fromPath(filename).via(unTarFlow(targetDir, system)).runWith(Sink.seq(), system);
  }

  private static Flow<ByteString, TarArchiveMetadata, NotUsed> unTarFlow(
      Path targetDir, ClassicActorSystemProvider system) {
    return Archive.tarReader()
        .mapAsync(
            1,
            pair -> {
              TarArchiveMetadata metadata = pair.first();
              Source<ByteString, NotUsed> source = pair.second();
              Path targetFile = targetDir.resolve(metadata.filePath());
              CompletionStage<List<TarArchiveMetadata>> readMetadata;
              if (metadata.isDirectory()) {
                readMetadata =
                    Source.single(targetFile)
                        .via(Directory.mkdirs())
                        .toMat(Sink.ignore(), Keep.right())
                        .run(system)
                        .thenApply(d -> Collections.singletonList(metadata));
              } else if (targetFile.getFileName().toString().endsWith(TARGZ_EXT)) {
                Path targetSubDir =
                    targetFile
                        .getParent()
                        .resolve(
                            Paths.get(
                                targetFile
                                    .getFileName()
                                    .toString()
                                    .substring(0, TARGZ_EXT.length() - 2)));
                readMetadata =
                    source
                        .via(Compression.gunzip(MAX_GUNZIP_CHUNK_SIZE))
                        .via(unTarFlow(targetSubDir, system))
                        .runWith(Sink.seq(), system);
              } else {
                readMetadata =
                    source
                        .toMat(Sink.ignore(), Keep.right())
                        .run(system)
                        .thenApply(d -> Collections.singletonList(metadata));
              }
              return readMetadata;
            })
        .flatMapConcat(Source::from);
  }
}
