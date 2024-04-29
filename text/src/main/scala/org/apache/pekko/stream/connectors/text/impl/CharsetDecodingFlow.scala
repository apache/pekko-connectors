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

package org.apache.pekko.stream.connectors.text.impl

import java.nio.charset.Charset

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.stage.{ GraphStage, GraphStageLogic }
import pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import pekko.util.ByteString

/**
 * Decodes a stream of bytes into a stream of characters, using a supplied [[java.nio.charset.Charset]].
 */
@InternalApi
private[text] class CharsetDecodingFlow(incoming: Charset) extends GraphStage[FlowShape[ByteString, String]] {
  final private val in = Inlet[ByteString]("in")
  final private val out = Outlet[String]("out")
  override val shape: FlowShape[ByteString, String] = FlowShape(in, out)

  def createLogic(attributes: Attributes): GraphStageLogic =
    new DecodingLogic(in, out, shape, incoming)
}
