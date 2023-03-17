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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl

import org.apache.pekko
import pekko.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import pekko.stream.Materializer
import pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord
import pekko.util.ByteString
import org.apache.avro.Schema
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.{ GenericDatumReader, GenericRecord }
import org.apache.avro.io.DecoderFactory

import java.util
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContext, Future }
import org.apache.pekko.util.ccompat.JavaConverters._

class AvroByteStringDecoder(schema: Schema) extends FromByteStringUnmarshaller[java.util.List[BigQueryRecord]] {

  val datumReader = new GenericDatumReader[GenericRecord](schema)

  override def apply(value: ByteString)(implicit ec: ExecutionContext,
      materializer: Materializer): Future[util.List[BigQueryRecord]] = {

    val result = ListBuffer[BigQueryRecord]()

    val inputStream = new SeekableByteArrayInput(value.toByteBuffer.array())
    val decoder = DecoderFactory.get.binaryDecoder(inputStream, null)
    while (!decoder.isEnd) {
      val item = datumReader.read(null, decoder)

      result += BigQueryRecord.fromAvro(item)
    }

    Future(result.toList.asJava)
  }

}
