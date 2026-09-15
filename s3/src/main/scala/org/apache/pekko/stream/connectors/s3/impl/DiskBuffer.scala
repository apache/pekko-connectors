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

package org.apache.pekko.stream.connectors.s3.impl

import java.io.{ File, FileOutputStream }
import java.nio.BufferOverflowException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.ActorAttributes
import pekko.stream.Attributes
import pekko.stream.FlowShape
import pekko.stream.Inlet
import pekko.stream.Outlet
import pekko.stream.scaladsl.FileIO
import pekko.stream.stage.GraphStageLogic
import pekko.stream.stage.InHandler
import pekko.stream.stage.OutHandler
import pekko.util.ByteString

import scala.concurrent.ExecutionContext

/**
 * Internal Api
 *
 * Tracks the temp files of chunks that have been emitted but not yet released, so that they can be deleted
 * as soon as they are no longer needed, and at the latest when the stream they were emitted into ends.
 */
@InternalApi private[impl] final class TempFileRegistry {
  private val files = ConcurrentHashMap.newKeySet[File]()

  def register(file: File): Unit = { files.add(file): Unit }

  /** Deletes the file and stops tracking it. Deleting a file that is already gone is a no-op. */
  def release(file: File): Unit = {
    files.remove(file)
    file.delete(): Unit
  }

  def releaseAll(): Unit = {
    files.forEach { file => file.delete(): Unit }
    files.clear()
  }
}

/**
 * Internal Api
 *
 * Buffers the complete incoming stream into a file, which can then be read several times afterwards.
 * ``
 * The stage waits for the incoming stream to complete. After that, it emits a single Chunk item on its output. The Chunk
 * contains a bytestream source that can be materialized multiple times, and the total size of the file.
 *
 * @param maxMaterializations Maximum number of materializations the completed chunk may see, which is reached only
 *                            when every upload retry is used. After this, the temp file is deleted. In the ordinary
 *                            case the chunk is disposed of as soon as the upload it belongs to is finished with it.
 * @param maxSize Maximum size on disk to buffer
 */
@InternalApi private[impl] final class DiskBuffer(maxMaterializations: Int, maxSize: Int, tempPath: Option[Path])
    extends ChunkBuffer {
  require(maxMaterializations > 0, "maxMaterializations should be at least 1")
  require(maxSize > 0, "maximumSize should be at least 1")

  private val registry = new TempFileRegistry

  override def cleanUp(): Unit = registry.releaseAll()

  val in = Inlet[ByteString]("DiskBuffer.in")
  val out = Outlet[Chunk]("DiskBuffer.out")
  override val shape = FlowShape.of(in, out)

  override def initialAttributes =
    super.initialAttributes and Attributes.name("DiskBuffer") and ActorAttributes.IODispatcher

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler {
      val path: File = tempPath
        .map(dir => Files.createTempFile(dir, "s3-buffer-", ".bin"))
        .getOrElse(Files.createTempFile("s3-buffer-", ".bin"))
        .toFile
      var length = 0
      val pathOut = new FileOutputStream(path)

      override def onPull(): Unit = if (isClosed(in)) emit() else pull(in)

      override def onPush(): Unit = {
        val elem = grab(in)
        length += elem.size
        if (length > maxSize) {
          throw new BufferOverflowException()
        }

        pathOut.write(elem.toArrayUnsafe())
        pull(in)
      }

      private var emitted = false

      override def onUpstreamFinish(): Unit = {
        if (isAvailable(out)) emit()
        completeStage()
      }

      override def postStop(): Unit =
        // close stream even if we didn't emit
        try {
          pathOut.close()
        } catch { case x: Throwable => () }
        finally {
          // nothing downstream can reference the file if the chunk was never emitted
          if (!emitted) path.delete(): Unit
        }

      private def emit(): Unit = {
        pathOut.close()
        emitted = true

        registry.register(path)

        val deleteCounter = new AtomicInteger(maxMaterializations)
        val src = FileIO.fromPath(path.toPath, 65536).mapMaterializedValue { f =>
          if (deleteCounter.decrementAndGet() <= 0)
            f.onComplete { _ =>
              registry.release(path)

            }(ExecutionContext.parasitic)
          NotUsed
        }
        emit(out, new DiskChunk(src, length, path, registry), () => completeStage())
      }
      setHandlers(in, out, this)
    }
}
