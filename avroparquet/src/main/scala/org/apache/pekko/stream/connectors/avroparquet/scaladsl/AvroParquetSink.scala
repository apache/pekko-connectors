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

package org.apache.pekko.stream.connectors.avroparquet.scaladsl

import org.apache.pekko
import pekko.Done
import pekko.stream.scaladsl.{ Flow, Keep, Sink }
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetWriter

import scala.concurrent.Future

object AvroParquetSink {

  def apply[T <: GenericRecord](writer: ParquetWriter[T]): Sink[T, Future[Done]] =
    Flow.fromGraph(new pekko.stream.connectors.avroparquet.impl.AvroParquetFlow(writer)).toMat(Sink.ignore)(
      Keep.right)

}
