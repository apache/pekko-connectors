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

// #mkdir-source
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.ftp.FtpSettings;
import org.apache.pekko.stream.connectors.ftp.javadsl.Ftp;
import org.apache.pekko.stream.javadsl.Source;

public class FtpMkdirExample {
  public Source<Done, NotUsed> mkdir(
      String parentPath, String directoryName, FtpSettings settings) {
    return Ftp.mkdir(parentPath, directoryName, settings);
  }
}

// #mkdir-source
