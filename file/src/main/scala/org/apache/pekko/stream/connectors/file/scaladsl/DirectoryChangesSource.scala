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

package org.apache.pekko.stream.connectors.file.scaladsl

import java.nio.file.Path
import java.util.function.BiFunction

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.file.DirectoryChange
import pekko.stream.scaladsl.Source

import scala.concurrent.duration.FiniteDuration

object DirectoryChangesSource {

  private val tupler = new BiFunction[Path, DirectoryChange, (Path, DirectoryChange)] {
    override def apply(t: Path, u: DirectoryChange): (Path, DirectoryChange) = (t, u)
  }

  /**
   * Watch directory and emit changes as a stream of tuples containing the path and type of change.
   *
   * @param directoryPath Directory to watch
   * @param pollInterval  Interval between polls to the JDK watch service when a push comes in and there was no changes, if
   *                      the JDK implementation is slow, it will not help lowering this
   * @param maxBufferSize Maximum number of buffered directory changes before the stage fails
   */
  def apply(directoryPath: Path,
      pollInterval: FiniteDuration,
      maxBufferSize: Int): Source[(Path, DirectoryChange), NotUsed] =
    Source.fromGraph(
      new pekko.stream.connectors.file.impl.DirectoryChangesSource(directoryPath, pollInterval,
        maxBufferSize, tupler))

}
