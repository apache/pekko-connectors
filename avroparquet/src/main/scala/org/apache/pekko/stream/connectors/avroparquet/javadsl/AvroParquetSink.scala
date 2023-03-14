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

package org.apache.pekko.stream.connectors.avroparquet.javadsl

import java.util.concurrent.CompletionStage
import org.apache.pekko.stream.javadsl.{ Flow, Keep, Sink }
import org.apache.pekko.{ Done, NotUsed }
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetWriter

object AvroParquetSink {

  def create[T <: GenericRecord](writer: ParquetWriter[T]): Sink[T, CompletionStage[Done]] =
    Flow
      .fromGraph(new org.apache.pekko.stream.connectors.avroparquet.impl.AvroParquetFlow(writer: ParquetWriter[T]))
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

}
