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

// #traversing
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.ftp.FtpSettings;
import org.apache.pekko.stream.connectors.ftp.javadsl.Ftp;

public class FtpTraversingExample {

  public void listFiles(String basePath, FtpSettings settings, ActorSystem system)
      throws Exception {
    Ftp.ls(basePath, settings)
        .runForeach(ftpFile -> System.out.println(ftpFile.toString()), system);
  }
}
// #traversing
