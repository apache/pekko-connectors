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

package org.apache.pekko.stream.connectors.xml.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import pekko.stream.connectors.xml.{ Characters, ParseEvent, TextEvent }
import pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }

/**
 * INTERNAL API
 */
@InternalApi private[xml] class Coalesce(maximumTextLength: Int) extends GraphStage[FlowShape[ParseEvent, ParseEvent]] {
  val in: Inlet[ParseEvent] = Inlet("XMLCoalesce.in")
  val out: Outlet[ParseEvent] = Outlet("XMLCoalesce.out")
  override val shape: FlowShape[ParseEvent, ParseEvent] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      private var isBuffering = false
      private val buffer = new StringBuilder

      override def onPull(): Unit = pull(in)

      override def onPush(): Unit = grab(in) match {
        case t: TextEvent =>
          if (t.text.length + buffer.length > maximumTextLength)
            failStage(
              new IllegalStateException(
                s"Too long character sequence, maximum is $maximumTextLength but got " +
                s"${t.text.length + buffer.length - maximumTextLength} more "))
          else {
            buffer.append(t.text)
            isBuffering = true
            pull(in)
          }
        case other =>
          if (isBuffering) {
            val coalesced = buffer.toString()
            isBuffering = false
            buffer.clear()
            emit(out, Characters(coalesced), () => emit(out, other, () => if (isClosed(in)) completeStage()))
          } else {
            push(out, other)
          }
      }

      override def onUpstreamFinish(): Unit =
        if (isBuffering) emit(out, Characters(buffer.toString()), () => completeStage())
        else completeStage()

      setHandlers(in, out, this)
    }
}
