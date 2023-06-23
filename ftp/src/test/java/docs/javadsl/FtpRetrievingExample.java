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

// #retrieving
import org.apache.pekko.stream.IOResult;
import org.apache.pekko.stream.connectors.ftp.FtpSettings;
import org.apache.pekko.stream.connectors.ftp.javadsl.Ftp;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import java.util.concurrent.CompletionStage;

public class FtpRetrievingExample {

  public Source<ByteString, CompletionStage<IOResult>> retrieveFromPath(
      String path, FtpSettings settings) throws Exception {
    return Ftp.fromPath(path, settings);
  }
}
// #retrieving
