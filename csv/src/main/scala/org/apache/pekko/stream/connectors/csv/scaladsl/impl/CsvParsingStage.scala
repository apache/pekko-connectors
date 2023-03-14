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

package org.apache.pekko.stream.connectors.csv.impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.event.Logging
import org.apache.pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import org.apache.pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import org.apache.pekko.util.ByteString

import scala.annotation.tailrec
import scala.util.control.NonFatal

/**
 * Internal API: Use [[org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing]] instead.
 */
@InternalApi private[csv] class CsvParsingStage(delimiter: Byte,
    quoteChar: Byte,
    escapeChar: Byte,
    maximumLineLength: Int)
    extends GraphStage[FlowShape[ByteString, List[ByteString]]] {

  private val in = Inlet[ByteString](Logging.simpleName(this) + ".in")
  private val out = Outlet[List[ByteString]](Logging.simpleName(this) + ".out")
  override val shape = FlowShape(in, out)

  override protected def initialAttributes: Attributes = Attributes.name("CsvParsing")

  override def createLogic(inheritedAttributes: Attributes) =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      private[this] val buffer = new CsvParser(delimiter, quoteChar, escapeChar, maximumLineLength)

      setHandlers(in, out, this)

      override def onPush(): Unit = {
        buffer.offer(grab(in))
        tryPollBuffer()
      }

      override def onPull(): Unit =
        tryPollBuffer()

      override def onUpstreamFinish(): Unit = {
        emitRemaining()
        completeStage()
      }

      private def tryPollBuffer() =
        try buffer.poll(requireLineEnd = true) match {
            case Some(csvLine) => push(out, csvLine)
            case _ =>
              if (isClosed(in)) {
                emitRemaining()
                completeStage()
              } else pull(in)
          }
        catch {
          case NonFatal(ex) => failStage(ex)
        }

      @tailrec private def emitRemaining(): Unit =
        buffer.poll(requireLineEnd = false) match {
          case Some(csvLine) =>
            emit(out, csvLine)
            emitRemaining()
          case _ =>
        }

    }
}
