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

// #retrieving-with-unconfirmed-reads

import akka.stream.IOResult;
import akka.stream.alpakka.ftp.SftpSettings;
import akka.stream.alpakka.ftp.javadsl.Sftp;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.util.concurrent.CompletionStage;

public class SftpRetrievingExample {

  public Source<ByteString, CompletionStage<IOResult>> retrieveFromPath(
      String path, SftpSettings settings) throws Exception {
    return Sftp.fromPath(path, settings.withMaxUnconfirmedReads(64));
  }
}
// #retrieving-with-unconfirmed-reads
