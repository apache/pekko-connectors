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

package org.apache.pekko.stream.connectors.mqtt.streaming
package impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import org.apache.pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import org.apache.pekko.util.ByteString

import scala.annotation.tailrec
import scala.collection.immutable

@InternalApi private[streaming] object MqttFrameStage {
  @tailrec
  def frames(
      maxPacketSize: Int,
      bytesReceived: ByteString,
      bytesToEmit: Vector[ByteString]): Either[IllegalStateException, (immutable.Iterable[ByteString], ByteString)] = {
    import MqttCodec._

    val i = bytesReceived.iterator
    val _ = i.drop(1) // Length starts at offset 1
    i.decodeRemainingLength() match {
      case Right(remainingLength) =>
        val headerSize = bytesReceived.size - i.len
        val packetSize = remainingLength + headerSize
        if (packetSize <= maxPacketSize) {
          if (bytesReceived.size >= packetSize) {
            val (b0, b1) = bytesReceived.splitAt(packetSize)
            frames(maxPacketSize, b1, bytesToEmit :+ b0)
          } else {
            Right((bytesToEmit, bytesReceived))
          }
        } else {
          Left(new IllegalStateException(s"Max packet size of $maxPacketSize exceeded with $packetSize"))
        }
      case _: Left[BufferUnderflow.type, Int] @unchecked =>
        Right((bytesToEmit, bytesReceived))
    }
  }
}

/*
 * Handles the framing of MQTT streams given that a length byte starts at an offset of 1 and
 * can then continue to be specified given the setting of a high bit, up to a maximum of
 * 4 bytes.
 *
 * 2.2.3 Remaining Length
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html
 */
@InternalApi private[streaming] final class MqttFrameStage(maxPacketSize: Int)
    extends GraphStage[FlowShape[ByteString, ByteString]] {

  import MqttFrameStage._

  private val in = Inlet[ByteString]("MqttFrame.in")
  private val out = Outlet[ByteString]("MqttFrame.out")

  override def shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(
        in,
        new InHandler {
          private var bytesReceived = ByteString.empty

          override def onPush(): Unit = {
            val bytes = grab(in)
            bytesReceived = bytesReceived ++ bytes

            frames(maxPacketSize, bytesReceived, Vector.empty) match {
              case Right((framed, remaining)) =>
                emitMultiple(out, framed)
                bytesReceived = remaining
                if (!hasBeenPulled(in)) pull(in)
              case Left(ex) =>
                failStage(ex)
            }
          }
        })

      setHandler(out,
        new OutHandler {
          override def onPull(): Unit =
            if (!hasBeenPulled(in)) pull(in)
        })
    }
}
