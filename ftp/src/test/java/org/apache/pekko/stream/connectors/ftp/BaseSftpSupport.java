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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BaseSftpSupport extends BaseSupportImpl {

  private final Path ROOT_DIR = Paths.get("tmp/home");
  final String HOSTNAME = "localhost";
  final int PORT = 2222;
  // Issue: the root folder of the sftp server is not writable so tests must happen inside a
  // sub-folder
  final String ROOT_PATH = "upload/";

  public static final byte[] CLIENT_PRIVATE_KEY_PASSPHRASE =
      "secret".getBytes(StandardCharsets.UTF_8);

  private File clientPrivateKeyFile;
  private File knownHostsFile;

  BaseSftpSupport() {
    clientPrivateKeyFile = new File(getClass().getResource("/id_rsa").getPath());
    knownHostsFile = new File(getClass().getResource("/known_hosts").getPath());
  }

  public File getClientPrivateKeyFile() {
    return clientPrivateKeyFile;
  }

  public File getKnownHostsFile() {
    return knownHostsFile;
  }

  @Override
  public Path getRootDir() {
    return ROOT_DIR;
  }
}
