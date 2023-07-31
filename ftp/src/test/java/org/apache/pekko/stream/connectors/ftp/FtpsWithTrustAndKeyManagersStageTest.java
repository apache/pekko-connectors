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

package org.apache.pekko.stream.connectors.ftp;

import nl.altindag.ssl.util.PemUtils;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.IOResult;
import org.apache.pekko.stream.connectors.ftp.javadsl.Ftps;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import javax.net.ssl.*;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class FtpsWithTrustAndKeyManagersStageTest extends BaseFtpSupport implements CommonFtpStageTest {
  private static final String PEM_PATH = "ftpd/pure-ftpd.pem";

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private X509ExtendedKeyManager keyManager;
  private X509ExtendedTrustManager trustManager;

  @Test
  public void listFiles() throws Exception {
    CommonFtpStageTest.super.listFiles();

    verify(trustManager).checkServerTrusted(any(X509Certificate[].class), anyString(), any(Socket.class));
  }

  public Source<FtpFile, NotUsed> getBrowserSource(String basePath) throws Exception {
    return Ftps.ls(basePath, settings());
  }

  public Source<ByteString, CompletionStage<IOResult>> getIOSource(String path) throws Exception {
    return Ftps.fromPath(path, settings());
  }

  public Sink<ByteString, CompletionStage<IOResult>> getIOSink(String path) throws Exception {
    return Ftps.toPath(path, settings());
  }

  public Sink<FtpFile, CompletionStage<IOResult>> getRemoveSink() throws Exception {
    return Ftps.remove(settings());
  }

  public Sink<FtpFile, CompletionStage<IOResult>> getMoveSink(
      Function<FtpFile, String> destinationPath) throws Exception {
    return Ftps.move(destinationPath, settings());
  }

  private FtpsSettings settings() throws Exception {
    keyManager = keyManager();
    trustManager = trustManager();

    return FtpsSettings.create(InetAddress.getByName(HOSTNAME))
        .withPort(PORT)
        .withCredentials(CREDENTIALS)
        .withBinary(false)
        .withPassiveMode(true)
        .withTrustManager(trustManager)
        .withKeyManager(keyManager);
  }

  private X509ExtendedKeyManager keyManager() throws IOException {
    try (InputStream stream = classLoader().getResourceAsStream(PEM_PATH)) {
      X509ExtendedKeyManager manager = PemUtils.loadIdentityMaterial(stream);
      return Mockito.spy(manager);
    }
  }

  private X509ExtendedTrustManager trustManager() throws IOException {
    try (InputStream stream = classLoader().getResourceAsStream(PEM_PATH)) {
      X509ExtendedTrustManager manager = PemUtils.loadTrustMaterial(stream);
      return Mockito.spy(manager);
    }
  }

  private ClassLoader classLoader() {
    return FtpsWithTrustAndKeyManagersStageTest.class.getClassLoader();
  }
}
