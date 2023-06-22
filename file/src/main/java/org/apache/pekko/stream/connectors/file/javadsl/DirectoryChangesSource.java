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
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.file.DirectoryChange;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.JavaDurationConverters;

import java.nio.file.Path;

/**
 * Watches a file system directory and streams change events from it.
 *
 * <p>Note that the JDK watcher is notoriously slow on some platform (up to 1s after event actually
 * happened on OSX for example)
 */
public final class DirectoryChangesSource {

  /**
   * @param directoryPath Directory to watch
   * @param pollInterval Interval between polls to the JDK watch service when a push comes in and
   *     there was no changes, if the JDK implementation is slow, it will not help lowering this
   * @param maxBufferSize Maximum number of buffered directory changes before the stage fails
   */
  public static Source<Pair<Path, DirectoryChange>, NotUsed> create(
      Path directoryPath, java.time.Duration pollInterval, int maxBufferSize) {
    return Source.fromGraph(
        new org.apache.pekko.stream.connectors.file.impl.DirectoryChangesSource<>(
            directoryPath,
            JavaDurationConverters.asFiniteDuration(pollInterval),
            maxBufferSize,
            Pair::apply));
  }
}
