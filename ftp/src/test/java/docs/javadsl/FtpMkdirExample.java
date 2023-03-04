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

// #mkdir-source
import akka.Done;
import akka.NotUsed;
import akka.stream.alpakka.ftp.FtpSettings;
import akka.stream.alpakka.ftp.javadsl.Ftp;
import akka.stream.javadsl.Source;

public class FtpMkdirExample {
  public Source<Done, NotUsed> mkdir(
      String parentPath, String directoryName, FtpSettings settings) {
    return Ftp.mkdir(parentPath, directoryName, settings);
  }
}

// #mkdir-source
