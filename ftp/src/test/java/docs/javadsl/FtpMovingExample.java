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

// #moving
import akka.stream.IOResult;
import akka.stream.alpakka.ftp.FtpFile;
import akka.stream.alpakka.ftp.FtpSettings;
import akka.stream.alpakka.ftp.javadsl.Ftp;
import akka.stream.javadsl.Sink;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class FtpMovingExample {

  public Sink<FtpFile, CompletionStage<IOResult>> move(
      Function<FtpFile, String> destinationPath, FtpSettings settings) throws Exception {
    return Ftp.move(destinationPath, settings);
  }
}
// #moving
