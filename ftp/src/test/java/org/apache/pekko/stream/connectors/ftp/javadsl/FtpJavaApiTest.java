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

package org.apache.pekko.stream.connectors.ftp.javadsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.stream.connectors.ftp.*;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the FTP model without an FTP server. */
public class FtpJavaApiTest {

  @Test
  public void ftpFileFromJava() {
    Set<PosixFilePermission> permissions =
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    FtpFile file = FtpFile.create("name", "/path/name", false, 10L, 0L, permissions);
    assertEquals(permissions, file.getPermissions());
    assertEquals("name", file.name());
  }

  @Test
  public void settingsGetters() throws Exception {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy", 8080));
    InetAddress host = InetAddress.getByName("localhost");

    assertEquals(Optional.of(proxy), FtpSettings.create(host).withProxy(proxy).getProxy());

    FtpsSettings ftps = FtpsSettings.create(host);
    assertEquals(Optional.empty(), ftps.getProxy());
    assertEquals(Optional.empty(), ftps.getKeyManager());
    assertEquals(Optional.empty(), ftps.getTrustManager());

    KeyFileSftpIdentity identity = SftpIdentity.createFileSftpIdentity("key", "secret".getBytes());
    SftpSettings sftp =
        SftpSettings.create(host).withKnownHosts("known").withSftpIdentity(identity);
    assertEquals(Optional.of("known"), sftp.getKnownHosts());
    assertEquals(Optional.of(identity), sftp.getSftpIdentity());
    assertEquals(Optional.empty(), sftp.getProxy());
    assertArrayEquals("secret".getBytes(), identity.getPrivateKeyFilePassphrase().get());

    RawKeySftpIdentity raw =
        SftpIdentity.createRawSftpIdentity(new byte[] {1}, new byte[] {2}, new byte[] {3});
    assertArrayEquals(new byte[] {3}, raw.getPublicKey().get());
  }
}
