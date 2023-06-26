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
import org.apache.pekko.stream.javadsl.Framing;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.apache.pekko.util.JavaDurationConverters;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Java API
 *
 * <p>Read the entire contents of a file, and then when the end is reached, keep reading newly
 * appended data. Like the unix command `tail -f`.
 *
 * <p>Aborting the stage can be done by combining with a [[org.apache.pekko.stream.KillSwitch]]
 *
 * <p>To use the stage from Scala see the factory methods in {@link
 * org.apache.pekko.stream.connectors.file.scaladsl.FileTailSource}
 */
public final class FileTailSource {

  /**
   * Read the entire contents of a file as chunks of bytes and when the end is reached, keep reading
   * newly appended data. Like the unix command `tail -f` but for bytes.
   *
   * <p>Reading text lines can be done with the `createLines` factory methods or by composing with
   * other stages manually depending on your needs. Aborting the stage can be done by combining with
   * a [[org.apache.pekko.stream.KillSwitch]]
   *
   * @param path a file path to tail
   * @param maxChunkSize The max emitted size of the `ByteString`s
   * @param startingPosition Offset into the file to start reading
   * @param pollingInterval When the end has been reached, look for new content with this interval
   */
  public static Source<ByteString, NotUsed> create(
      Path path, int maxChunkSize, long startingPosition, java.time.Duration pollingInterval) {
    return Source.fromGraph(
        new org.apache.pekko.stream.connectors.file.impl.FileTailSource(
            path,
            maxChunkSize,
            startingPosition,
            JavaDurationConverters.asFiniteDuration(pollingInterval)));
  }

  /**
   * Read the entire contents of a file as text lines, and then when the end is reached, keep
   * reading newly appended data. Like the unix command `tail -f`.
   *
   * <p>If a line is longer than `maxChunkSize` the stream will fail.
   *
   * <p>Aborting the stage can be done by combining with a [[org.apache.pekko.stream.KillSwitch]]
   *
   * @param path a file path to tail
   * @param maxLineSize The max emitted size of the `ByteString`s
   * @param pollingInterval When the end has been reached, look for new content with this interval
   * @param lf The character or characters used as line separator
   * @param charset The charset of the file
   */
  public static Source<String, NotUsed> createLines(
      Path path, int maxLineSize, java.time.Duration pollingInterval, String lf, Charset charset) {
    return create(path, maxLineSize, 0, pollingInterval)
        .via(Framing.delimiter(ByteString.fromString(lf, charset.name()), maxLineSize))
        .map(bytes -> bytes.decodeString(charset));
  }

  /**
   * Same as {@link #createLines(Path, int, java.time.Duration, String, Charset)} but using the OS
   * default line separator and UTF-8 for charset
   */
  public static Source<String, NotUsed> createLines(
      Path path, int maxChunkSize, java.time.Duration pollingInterval) {
    return createLines(
        path,
        maxChunkSize,
        pollingInterval,
        System.getProperty("line.separator"),
        StandardCharsets.UTF_8);
  }
}
