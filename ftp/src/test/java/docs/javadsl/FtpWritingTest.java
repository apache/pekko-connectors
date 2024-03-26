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

// #storing
// #create-settings
import org.apache.pekko.stream.connectors.ftp.javadsl.Ftp;
// #create-settings
import org.apache.pekko.stream.IOResult;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Compression;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.util.ByteString;
import java.util.concurrent.CompletionStage;
// #storing
import java.io.PrintWriter;

// #create-settings
import org.apache.pekko.stream.connectors.ftp.FtpSettings;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import java.net.InetAddress;

// #create-settings
import org.apache.pekko.stream.connectors.ftp.BaseFtpSupport;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.*;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FtpWritingTest extends BaseFtpSupport {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @After
  public void afterEach() {
    StreamTestKit.assertAllStagesStopped(getMaterializer());
    TestKit.shutdownActorSystem(getSystem());
  }

  FtpSettings ftpSettings() throws Exception {
    // #create-settings
    FtpSettings ftpSettings =
        FtpSettings.create(InetAddress.getByName(HOSTNAME))
            .withPort(PORT)
            .withCredentials(CREDENTIALS)
            .withBinary(true)
            .withPassiveMode(true)
            // only useful for debugging
            .withConfigureConnectionConsumer(
                (FTPClient ftpClient) -> ftpClient.addProtocolCommandListener(
                      new PrintCommandListener(new PrintWriter(System.out), true)));
    // #create-settings
    return ftpSettings;
  }

  @Test
  public void targetFileShouldBeCreated() throws Exception {
    Materializer materializer = getMaterializer();
    FtpSettings ftpSettings = ftpSettings();
    // #storing

    CompletionStage<IOResult> result =
        Source.single(ByteString.fromString("this is the file contents"))
            .runWith(Ftp.toPath("file.txt", ftpSettings), materializer);
    // #storing

    IOResult ioResult = result.toCompletableFuture().get(5, TimeUnit.SECONDS);
    assertThat(ioResult, is(IOResult.createSuccessful(25)));
    assertTrue(fileExists("file.txt"));
  }

  @Test
  public void gZippedTargetFileShouldBeCreated() throws Exception {
    Materializer materializer = getMaterializer();
    FtpSettings ftpSettings = ftpSettings();
    // #storing

    // Create a gzipped target file
    CompletionStage<IOResult> result =
        Source.single(ByteString.fromString("this is the file contents"))
            .via(Compression.gzip())
            .runWith(Ftp.toPath("file.txt.gz", ftpSettings), materializer);
    // #storing

    IOResult ioResult = result.toCompletableFuture().get(5, TimeUnit.SECONDS);
    assertThat(ioResult, is(IOResult.createSuccessful(50)));
    assertTrue(fileExists("file.txt.gz"));
  }
}
