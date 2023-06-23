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

package org.apache.pekko.stream.connectors.avroparquet.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.javadsl.Source
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetReader

object AvroParquetSource {

  def create[T <: GenericRecord](reader: ParquetReader[T]): Source[T, NotUsed] =
    Source.fromGraph(new pekko.stream.connectors.avroparquet.impl.AvroParquetSource(reader))
}
