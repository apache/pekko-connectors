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

package org.apache.pekko.stream.connectors.s3.impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import org.apache.pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import org.apache.pekko.util.ByteString

/**
 * Internal Api
 *
 * Buffers the complete incoming stream into memory, which can then be read several times afterwards.
 *
 * The stage waits for the incoming stream to complete. After that, it emits a single Chunk item on its output. The Chunk
 * contains a `ByteString` source that can be materialized multiple times, and the total size of the file.
 *
 * @param maxSize Maximum size to buffer
 */
@InternalApi private[impl] final class MemoryBuffer(maxSize: Int) extends GraphStage[FlowShape[ByteString, Chunk]] {
  val in = Inlet[ByteString]("MemoryBuffer.in")
  val out = Outlet[Chunk]("MemoryBuffer.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      private var buffer = ByteString.empty

      override def onPull(): Unit = if (isClosed(in)) emit() else pull(in)

      override def onPush(): Unit = {
        val elem = grab(in)
        if (buffer.size + elem.size > maxSize) {
          failStage(new IllegalStateException("Buffer size of " + maxSize + " bytes exceeded."))
        } else {
          buffer ++= elem
          pull(in)
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (isAvailable(out)) emit()
        completeStage()
      }

      private def emit(): Unit = emit(out, MemoryChunk(buffer), () => completeStage())

      setHandlers(in, out, this)
    }

}
