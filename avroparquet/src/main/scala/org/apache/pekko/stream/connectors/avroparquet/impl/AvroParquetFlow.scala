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

package org.apache.pekko.stream.connectors.avroparquet.impl
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream._
import org.apache.pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetWriter

/**
 * Internal API
 */
@InternalApi
private[avroparquet] class AvroParquetFlow[T <: GenericRecord](writer: ParquetWriter[T])
    extends GraphStage[FlowShape[T, T]] {

  val in: Inlet[T] = Inlet("AvroParquetSink.in")
  val out: Outlet[T] = Outlet("AvroParquetSink.out")
  override val shape: FlowShape[T, T] = FlowShape.of(in, out)

  override protected def initialAttributes: Attributes =
    super.initialAttributes and ActorAttributes.IODispatcher

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(
        in,
        new InHandler {

          override def onUpstreamFinish(): Unit =
            // super.onUpstreamFinish()
            completeStage()

          override def onUpstreamFailure(ex: Throwable): Unit = {
            super.onUpstreamFailure(ex)
            writer.close()
          }

          override def onPush(): Unit = {
            val obtainedValue = grab(in)
            writer.write(obtainedValue)
            push(out, obtainedValue)
          }
        })

      setHandler(out,
        new OutHandler {
          override def onPull(): Unit =
            pull(in)
        })

      override def postStop(): Unit = writer.close()
    }
}
