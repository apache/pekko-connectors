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

package org.apache.pekko.stream.connectors.ftp;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.IOResult;
import org.apache.pekko.stream.connectors.ftp.javadsl.Sftp;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@ExtendWith(LogCapturingExtension.class)
public class SftpWithProxyStageTest extends BaseSftpSupport implements CommonFtpStageTest {

  private final Integer PROXYPORT = 3128;
  private final Proxy PROXY =
      new Proxy(Proxy.Type.HTTP, new InetSocketAddress(HOSTNAME, PROXYPORT));

  @Test
  public void listFiles() throws Exception {
    CommonFtpStageTest.super.listFiles();
  }

  @Test
  public void fromPath() throws Exception {
    CommonFtpStageTest.super.fromPath();
  }

  @Test
  public void toPath() throws Exception {
    CommonFtpStageTest.super.toPath();
  }

  @Test
  @Disabled("flakey, see https://github.com/akka/alpakka/issues/2126")
  public void remove() throws Exception {
    CommonFtpStageTest.super.remove();
  }

  @Test
  @Disabled("flakey, see https://github.com/akka/alpakka/issues/2126")
  public void move() throws Exception {
    CommonFtpStageTest.super.move();
  }

  public Source<FtpFile, NotUsed> getBrowserSource(String basePath) throws Exception {
    return Sftp.ls(ROOT_PATH + basePath, settings());
  }

  public Source<ByteString, CompletionStage<IOResult>> getIOSource(String path) throws Exception {
    return Sftp.fromPath(ROOT_PATH + path, settings());
  }

  public Sink<ByteString, CompletionStage<IOResult>> getIOSink(String path) throws Exception {
    return Sftp.toPath(ROOT_PATH + path, settings());
  }

  public Sink<FtpFile, CompletionStage<IOResult>> getRemoveSink() throws Exception {
    return Sftp.remove(settings());
  }

  public Sink<FtpFile, CompletionStage<IOResult>> getMoveSink(
      Function<FtpFile, String> destinationPath) throws Exception {
    return Sftp.move(f -> ROOT_PATH + destinationPath.apply(f), settings());
  }

  private SftpSettings settings() throws Exception {
    return SftpSettings.create(InetAddress.getByName(HOSTNAME))
        .withPort(PORT)
        .withCredentials(CREDENTIALS)
        .withStrictHostKeyChecking(false)
        .withProxy(PROXY);
  }
}
