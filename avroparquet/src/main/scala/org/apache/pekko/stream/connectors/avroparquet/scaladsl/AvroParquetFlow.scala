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
import pekko.NotUsed
import pekko.stream.scaladsl.Flow
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetWriter

object AvroParquetFlow {

  def apply[T <: GenericRecord](writer: ParquetWriter[T]): Flow[T, T, NotUsed] =
    Flow.fromGraph(new pekko.stream.connectors.avroparquet.impl.AvroParquetFlow(writer))
}
