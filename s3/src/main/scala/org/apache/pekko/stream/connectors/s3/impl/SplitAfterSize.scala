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
import org.apache.pekko.stream.scaladsl.SubFlow
import org.apache.pekko.stream.stage.GraphStage
import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.Inlet
import org.apache.pekko.stream.Outlet
import org.apache.pekko.stream.stage.GraphStageLogic
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.stage.OutHandler
import org.apache.pekko.stream.stage.InHandler
import org.apache.pekko.stream.scaladsl.Flow

import scala.annotation.tailrec

/**
 * Internal Api
 *
 * Splits up a byte stream source into sub-flows of a minimum size. Does not attempt to create chunks of an exact size.
 */
@InternalApi private[impl] object SplitAfterSize {
  def apply[I, M](minChunkSize: Int,
      maxChunkSize: Int)(in: Flow[I, ByteString, M]): SubFlow[ByteString, M, in.Repr, in.Closed] = {
    require(minChunkSize < maxChunkSize, "the min chunk size must be smaller than the max chunk size")
    in.via(insertMarkers(minChunkSize, maxChunkSize)).splitWhen(_ == NewStream).collect { case bs: ByteString => bs }
  }

  private case object NewStream

  private def insertMarkers(minChunkSize: Long, maxChunkSize: Int) = new GraphStage[FlowShape[ByteString, Any]] {
    val in = Inlet[ByteString]("SplitAfterSize.in")
    val out = Outlet[Any]("SplitAfterSize.out")
    override val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with OutHandler with InHandler {
        var count: Int = 0
        override def onPull(): Unit = pull(in)

        override def onPush(): Unit = {
          val elem = grab(in)
          count += elem.size
          if (count > maxChunkSize) {
            splitElement(elem, elem.size - (count - maxChunkSize))
          } else if (count >= minChunkSize) {
            count = 0
            emitMultiple(out, elem :: NewStream :: Nil)
          } else emit(out, elem)
        }

        @tailrec private def splitElement(elem: ByteString, splitPos: Int): Unit =
          if (elem.size > splitPos) {
            val (part1, rest) = elem.splitAt(splitPos)
            emitMultiple(out, part1 :: NewStream :: Nil)
            splitElement(rest, maxChunkSize)
          } else {
            count = elem.size
            emit(out, elem)
          }

        setHandlers(in, out, this)
      }
  }
}
