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

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import pekko.util.ByteString

/**
 * INTERNAL API
 */
@InternalApi private[file] class EnsureByteStreamSize(expectedSize: Long)
    extends GraphStage[FlowShape[ByteString, ByteString]] {

  val in = Inlet[ByteString]("EnsureByteStreamSize.in")
  val out = Outlet[ByteString]("EnsureByteStreamSize.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var currentSize = 0L

    setHandler(
      in,
      new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          currentSize = currentSize + elem.size
          push(out, elem)
        }

        override def onUpstreamFinish(): Unit = {
          if (currentSize == expectedSize) super.onUpstreamFinish()
          else failStage(new IllegalStateException(s"Expected ${expectedSize} bytes but got ${currentSize} bytes"))
        }
      })
    setHandler(out,
      new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
  }

}
