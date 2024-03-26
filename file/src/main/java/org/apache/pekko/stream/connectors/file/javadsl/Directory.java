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

package org.apache.pekko.stream.connectors.file.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.FlowWithContext;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.javadsl.StreamConverters;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Directory {

  /** List all files in the given directory */
  public static Source<Path, NotUsed> ls(Path directory) {
    return org.apache.pekko.stream.connectors.file.scaladsl.Directory.ls(directory).asJava();
  }

  /** Recursively list files and directories in the given directory, depth first. */
  public static Source<Path, NotUsed> walk(Path directory) {
    return StreamConverters.fromJavaStream(() -> Files.walk(directory));
  }

  /**
   * Recursively list files and directories in the given directory, depth first, with a maximum
   * directory depth limit and a possibly set of options (See {@link java.nio.file.Files#walk} for
   * details).
   */
  public static Source<Path, NotUsed> walk(
      Path directory, int maxDepth, FileVisitOption... options) {
    return StreamConverters.fromJavaStream(() -> Files.walk(directory, maxDepth, options));
  }

  /** Create local directories, including any parent directories. */
  public static Flow<Path, Path, NotUsed> mkdirs() {
    return org.apache.pekko.stream.connectors.file.scaladsl.Directory.mkdirs().asJava();
  }

  /**
   * Create local directories, including any parent directories. Passes arbitrary data as context.
   */
  public static <Ctx> FlowWithContext<Path, Ctx, Path, Ctx, NotUsed> mkdirsWithContext() {
    return org.apache.pekko.stream.connectors.file.scaladsl.Directory.mkdirsWithContext().asJava();
  }
}
