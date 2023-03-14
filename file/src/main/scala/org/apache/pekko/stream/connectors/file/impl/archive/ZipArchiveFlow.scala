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

package org.apache.pekko.stream.connectors.file.impl.archive

import java.util.zip.{ ZipEntry, ZipOutputStream }

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.event.Logging
import org.apache.pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import org.apache.pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import org.apache.pekko.util.{ ByteString, ByteStringBuilder }

/**
 * INTERNAL API
 */
@InternalApi private[file] final class ZipArchiveFlowStage(
    val shape: FlowShape[ByteString, ByteString]) extends GraphStageLogic(shape) {

  private def in = shape.in
  private def out = shape.out

  private val builder = new ByteStringBuilder()
  private val zip = new ZipOutputStream(builder.asOutputStream)
  private var emptyStream = true

  setHandler(
    out,
    new OutHandler {
      override def onPull(): Unit =
        if (isClosed(in)) {
          emptyStream = true
          completeStage()
        } else {
          pull(in)
        }
    })

  setHandler(
    in,
    new InHandler {
      override def onPush(): Unit = {
        emptyStream = false
        val element = grab(in)
        element match {
          case b: ByteString if FileByteStringSeparators.isStartingByteString(b) =>
            val name = FileByteStringSeparators.getPathFromStartingByteString(b)
            zip.putNextEntry(new ZipEntry(name))
          case b: ByteString if FileByteStringSeparators.isEndingByteString(b) =>
            zip.closeEntry()
          case b: ByteString =>
            val array = b.toArray
            zip.write(array, 0, array.length)
        }
        zip.flush()
        val result = builder.result()
        if (result.nonEmpty) {
          builder.clear()
          push(out, result)
        } else {
          pull(in)
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (!emptyStream) {
          zip.close()
          emit(out, builder.result())
          builder.clear()
        }
        super.onUpstreamFinish()
      }

    })

}

/**
 * INTERNAL API
 */
@InternalApi private[file] final class ZipArchiveFlow extends GraphStage[FlowShape[ByteString, ByteString]] {

  val in: Inlet[ByteString] = Inlet(Logging.simpleName(this) + ".in")
  val out: Outlet[ByteString] = Outlet(Logging.simpleName(this) + ".out")

  override def initialAttributes: Attributes =
    Attributes.name(Logging.simpleName(this))

  override val shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new ZipArchiveFlowStage(shape)
}
