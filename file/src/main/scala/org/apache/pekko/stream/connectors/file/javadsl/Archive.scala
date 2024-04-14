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

package org.apache.pekko.stream.connectors.file.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.file.{ scaladsl, ArchiveMetadata, TarArchiveMetadata, ZipArchiveMetadata }
import pekko.stream.javadsl.Flow
import pekko.util.ByteString
import pekko.japi.Pair
import pekko.stream.connectors.file.impl.archive.{ TarReaderStage, ZipSource }
import pekko.stream.javadsl.Source

import java.io.File
import java.nio.charset.{ Charset, StandardCharsets }

/**
 * Java API.
 */
object Archive {

  /**
   * Flow for compressing multiple files into one ZIP file.
   */
  def zip(): Flow[Pair[ArchiveMetadata, Source[ByteString, NotUsed]], ByteString, NotUsed] =
    Flow
      .create[Pair[ArchiveMetadata, Source[ByteString, NotUsed]]]()
      .map(func(pair => (pair.first, pair.second.asScala)))
      .via(scaladsl.Archive.zip().asJava)

  /**
   * Flow for reading ZIP files.
   */
  def zipReader(
      file: File,
      chunkSize: Int,
      fileCharset: Charset): Source[Pair[ZipArchiveMetadata, Source[ByteString, NotUsed]], NotUsed] =
    Source
      .fromGraph(new ZipSource(file, chunkSize, fileCharset))
      .map(func {
        case (metadata, source) =>
          Pair(metadata, source.asJava)
      })
  def zipReader(file: File): Source[Pair[ZipArchiveMetadata, Source[ByteString, NotUsed]], NotUsed] =
    zipReader(file, 8192)
  def zipReader(
      file: File,
      chunkSize: Int): Source[Pair[ZipArchiveMetadata, Source[ByteString, NotUsed]], NotUsed] =
    Source
      .fromGraph(new ZipSource(file, chunkSize, StandardCharsets.UTF_8))
      .map(func {
        case (metadata, source) =>
          Pair(metadata, source.asJava)
      })

  /**
   * Flow for packaging multiple files into one TAR file.
   */
  def tar(): Flow[Pair[TarArchiveMetadata, Source[ByteString, NotUsed]], ByteString, NotUsed] =
    Flow
      .create[Pair[TarArchiveMetadata, Source[ByteString, NotUsed]]]()
      .map(func(pair => (pair.first, pair.second.asScala)))
      .via(scaladsl.Archive.tar().asJava)

  /**
   * Parse incoming `ByteString`s into tar file entries and sources for the file contents.
   * The file contents sources MUST be consumed to progress reading the file.
   */
  def tarReader(): Flow[ByteString, Pair[TarArchiveMetadata, Source[ByteString, NotUsed]], NotUsed] =
    Flow
      .fromGraph(new TarReaderStage())
      .map(func {
        case (metadata, source) =>
          Pair(metadata, source.asJava)
      })

  private def func[T, R](f: T => R): pekko.japi.function.Function[T, R] = (param: T) => f(param)
}
