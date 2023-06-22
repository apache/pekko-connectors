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

import java.nio.file.Path;
import java.nio.file.Paths;

public class BaseFtpSupport extends BaseSupportImpl {

  private final Path ROOT_DIR = Paths.get("tmp/home");
  public final String HOSTNAME = "localhost";
  public final int PORT = 21000;

  @Override
  public Path getRootDir() {
    return ROOT_DIR;
  }
}
