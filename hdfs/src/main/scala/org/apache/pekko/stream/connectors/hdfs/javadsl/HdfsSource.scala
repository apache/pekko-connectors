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

package org.apache.pekko.stream.connectors.hdfs.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.NotUsed
import pekko.japi.Pair
import pekko.stream.connectors.hdfs.scaladsl.{ HdfsSource => ScalaHdfsSource }
import pekko.stream.{ javadsl, IOResult }
import pekko.util.ByteString
import pekko.util.FutureConverters._
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.hadoop.io.Writable
import org.apache.hadoop.io.compress.CompressionCodec

object HdfsSource {

  /**
   * Java API: creates a [[javadsl.Source]] that consumes as [[ByteString]]
   *
   * @param fs Hadoop file system
   * @param path the file to open
   */
  def data(
      fs: FileSystem,
      path: Path): javadsl.Source[ByteString, CompletionStage[IOResult]] =
    ScalaHdfsSource.data(fs, path).mapMaterializedValue(_.asJava).asJava

  /**
   * Java API: creates a [[javadsl.Source]] that consumes as [[ByteString]]
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param chunkSize the size of each read operation, defaults to 8192
   */
  def data(
      fs: FileSystem,
      path: Path,
      chunkSize: Int): javadsl.Source[ByteString, CompletionStage[IOResult]] =
    ScalaHdfsSource.data(fs, path, chunkSize).mapMaterializedValue(_.asJava).asJava

  /**
   * Java API: creates a [[javadsl.Source]] that consumes as [[ByteString]]
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param codec a streaming compression/decompression pair
   */
  def compressed(
      fs: FileSystem,
      path: Path,
      codec: CompressionCodec): javadsl.Source[ByteString, CompletionStage[IOResult]] =
    ScalaHdfsSource.compressed(fs, path, codec).mapMaterializedValue(_.asJava).asJava

  /**
   * Java API: creates a [[javadsl.Source]] that consumes as [[ByteString]]
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param codec a streaming compression/decompression pair
   * @param chunkSize the size of each read operation, defaults to 8192
   */
  def compressed(
      fs: FileSystem,
      path: Path,
      codec: CompressionCodec,
      chunkSize: Int = 8192): javadsl.Source[ByteString, CompletionStage[IOResult]] =
    ScalaHdfsSource.compressed(fs, path, codec, chunkSize).mapMaterializedValue(_.asJava).asJava

  /**
   * Java API: creates a [[javadsl.Source]] that consumes as [[(K, V)]]
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param classK a key class
   * @param classV a value class
   */
  def sequence[K <: Writable, V <: Writable](
      fs: FileSystem,
      path: Path,
      classK: Class[K],
      classV: Class[V]): javadsl.Source[Pair[K, V], NotUsed] =
    ScalaHdfsSource.sequence(fs, path, classK, classV).map { case (k, v) => new Pair(k, v) }.asJava

}
