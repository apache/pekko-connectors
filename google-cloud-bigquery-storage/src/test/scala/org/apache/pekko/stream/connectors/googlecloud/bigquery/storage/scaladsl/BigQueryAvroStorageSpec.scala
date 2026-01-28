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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl

import org.apache.pekko
import pekko.stream.connectors.googlecloud.bigquery.storage.impl.AvroDecoder
import pekko.stream.connectors.googlecloud.bigquery.storage.{
  BigQueryRecord,
  BigQueryStorageSettings,
  BigQueryStorageSpecBase
}
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Sink
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class BigQueryAvroStorageSpec
    extends BigQueryStorageSpecBase(21002)
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with LogCapturing {

  "BigQueryAvroStorage.readAvro" should {
    val avroSchema = storageAvroSchema.value
    val avroRows = storageAvroRows.value

    "stream the results for a query in records merged" in {
      val decoder = AvroDecoder(avroSchema.schema)
      val records = decoder.decodeRows(avroRows.serializedBinaryRows).map(gr => BigQueryRecord.fromAvro(gr))

      BigQueryAvroStorage
        .readRecordsMerged(Project, Dataset, Table, None)
        .withAttributes(mockBQReader())
        .runWith(Sink.seq)
        .futureValue shouldBe Seq.fill(DefaultNumStreams * ResponsesPerStream)(records)
    }

    "stream the results for a query in records" in {
      val decoder = AvroDecoder(avroSchema.schema)
      val records = decoder.decodeRows(avroRows.serializedBinaryRows).map(gr => BigQueryRecord.fromAvro(gr))

      BigQueryAvroStorage
        .readRecords(Project, Dataset, Table, None)
        .withAttributes(mockBQReader())
        .map(a => a.reduce((a, b) => a.merge(b)))
        .flatMapMerge(100, identity)
        .runWith(Sink.seq)
        .futureValue shouldBe Seq.fill(DefaultNumStreams * ResponsesPerStream)(records).flatten
    }

    "stream the results for a query merged" in {
      BigQueryAvroStorage
        .readMerged(Project, Dataset, Table, None)
        .withAttributes(mockBQReader())
        .map(s => s._2.map(b => (s._1, b)))
        .flatMapMerge(100, identity)
        .runWith(Sink.seq)
        .futureValue shouldBe Vector.fill(DefaultNumStreams * ResponsesPerStream)((avroSchema, avroRows))
    }

    "stream the results for a query" in {
      BigQueryAvroStorage
        .read(Project, Dataset, Table, None)
        .withAttributes(mockBQReader())
        .map { s =>
          s._2
            .reduce((a, b) => a.merge(b))
            .map(b => (s._1, b))
        }
        .flatMapMerge(100, identity)
        .runWith(Sink.seq)
        .futureValue shouldBe Vector.fill(DefaultNumStreams * ResponsesPerStream)((avroSchema, avroRows))
    }
  }

  def mockBQReader(host: String = bqHost, port: Int = bqPort) = {
    val reader = GrpcBigQueryStorageReader(BigQueryStorageSettings(host, port))
    BigQueryStorageAttributes.reader(reader)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startMock()
  }

  override def afterAll(): Unit = {
    stopMock()
    system.terminate()
    super.afterAll()
  }

}
