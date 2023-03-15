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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord
import com.google.protobuf.ByteString
import org.apache.avro.Schema
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.{ GenericDatumReader, GenericRecord }
import org.apache.avro.io.DecoderFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Internal API
 */
@InternalApi private[bigquery] class AvroDecoder(schema: Schema) {
  val datumReader = new GenericDatumReader[GenericRecord](schema)

  def decodeToRecord(avroRows: ByteString): List[BigQueryRecord] = {
    val result = ListBuffer[BigQueryRecord]()

    val inputStream = new SeekableByteArrayInput(avroRows.toByteArray)
    val decoder = DecoderFactory.get.binaryDecoder(inputStream, null)
    while (!decoder.isEnd) {
      val item = datumReader.read(null, decoder)

      result += BigQueryRecord.fromAvro(item)
    }

    result.toList
  }

  def decodeRows(avroRows: ByteString): List[GenericRecord] = {
    val result = new mutable.ListBuffer[GenericRecord]

    val inputStream = new SeekableByteArrayInput(avroRows.toByteArray)
    val decoder = DecoderFactory.get.binaryDecoder(inputStream, null)
    while (!decoder.isEnd) {
      val item = datumReader.read(null, decoder)

      result += item
    }

    result.toList
  }
}

@InternalApi private[bigquery] object AvroDecoder {
  def apply(schema: String): AvroDecoder = new AvroDecoder(new Schema.Parser().parse(schema))

}
