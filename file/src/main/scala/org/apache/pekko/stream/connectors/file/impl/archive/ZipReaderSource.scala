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

package org.apache.pekko.stream.connectors.file.impl.archive

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.connectors.file.ZipArchiveMetadata
import pekko.stream.{ Attributes, Outlet, SourceShape }
import pekko.stream.scaladsl.Source
import pekko.stream.stage.{ GraphStage, GraphStageLogic, OutHandler }
import pekko.util.ByteString

import java.io.{ File, FileInputStream }
import java.nio.charset.{ Charset, StandardCharsets }
import java.util.zip.{ ZipEntry, ZipInputStream }

@InternalApi class ZipEntrySource(n: ZipArchiveMetadata, f: File, chunkSize: Int, fileCharset: Charset)
    extends GraphStage[SourceShape[ByteString]] {
  private val out = Outlet[ByteString]("flowOut")
  override val shape: SourceShape[ByteString] =
    SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      val zis = new ZipInputStream(new FileInputStream(f), fileCharset)
      var entry: ZipEntry = null
      val data = new Array[Byte](chunkSize)

      def seek() = {
        while ({
            entry = zis.getNextEntry()
            entry != null && entry.getName != n.name
          }) {
          zis.closeEntry()
        }
      }

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            if (entry == null) {
              seek()
              if (entry == null) {
                failStage(new Exception("After a seek the part is not found"))
              }
            }

            val c = zis.read(data, 0, chunkSize)
            if (c == -1) {
              completeStage()
            } else {
              push(out, ByteString.fromArray(data, 0, c))
            }
          }
        })

      override def postStop(): Unit = {
        super.postStop()
        zis.close()
      }
    }
}

@InternalApi class ZipSource(f: File, chunkSize: Int, fileCharset: Charset = StandardCharsets.UTF_8)
    extends GraphStage[SourceShape[(ZipArchiveMetadata, Source[ByteString, NotUsed])]] {
  private val out = Outlet[(ZipArchiveMetadata, Source[ByteString, NotUsed])]("flowOut")
  override val shape: SourceShape[(ZipArchiveMetadata, Source[ByteString, NotUsed])] =
    SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      val zis = new ZipInputStream(new FileInputStream(f), fileCharset)

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            val e = zis.getNextEntry
            if (e != null) {
              val n = ZipArchiveMetadata(e.getName)
              zis.closeEntry()
              push(out, n -> Source.fromGraph(new ZipEntrySource(n, f, chunkSize, fileCharset)))
            } else {
              zis.close()
              completeStage()
            }
          }
        })

      override def postStop(): Unit = {
        super.postStop()
        zis.close()
      }
    }
}
