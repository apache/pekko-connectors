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
import scala.util.control.NonFatal

@InternalApi private[archive] object ZipReaderSource {

  /**
   * Opens `f` as a zip stream, closing the underlying file if the zip stream itself cannot be opened.
   */
  def openZip(f: File, fileCharset: Charset): ZipInputStream = {
    val fis = new FileInputStream(f)
    try new ZipInputStream(fis, fileCharset)
    catch {
      case NonFatal(e) =>
        try fis.close()
        catch { case NonFatal(suppressed) => e.addSuppressed(suppressed) }
        throw e
    }
  }
}

@InternalApi class ZipEntrySource(n: ZipArchiveMetadata, f: File, chunkSize: Int, fileCharset: Charset)
    extends GraphStage[SourceShape[ByteString]] {
  private val out = Outlet[ByteString]("flowOut")
  override val shape: SourceShape[ByteString] =
    SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private var zis: ZipInputStream = _
      var entry: ZipEntry = null
      val data = new Array[Byte](chunkSize)

      override def preStart(): Unit = {
        super.preStart()
        zis = ZipReaderSource.openZip(f, fileCharset)
      }

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
        if (zis ne null) zis.close()
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
      private var zis: ZipInputStream = _

      override def preStart(): Unit = {
        super.preStart()
        zis = ZipReaderSource.openZip(f, fileCharset)
      }

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
        if (zis ne null) zis.close()
      }
    }
}
