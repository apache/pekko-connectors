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

package docs.scaladsl

import com.sksamuel.avro4s.Record
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.stream.connectors.avroparquet.scaladsl.AvroParquetFlow
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AvroParquetFlowSpec
    extends TestKit(ActorSystem("FlowSpec"))
    with AnyWordSpecLike
    with Matchers
    with AbstractAvroParquet
    with ScalaFutures
    with BeforeAndAfterAll {

  "Parquet Flow" should {

    "insert avro records in parquet from `GenericRecord`" in assertAllStagesStopped {
      // given
      val n: Int = 2
      val file: String = genFinalFile.sample.get
      // #init-flow
      val records: List[GenericRecord]
      // #init-flow
      = genDocuments(n).sample.get.map(docToGenericRecord)
      val writer: ParquetWriter[GenericRecord] = parquetWriter(file, conf, schema)

      // when
      // #init-flow
      val source: Source[GenericRecord, NotUsed] = Source(records)
      val avroParquet: Flow[GenericRecord, GenericRecord, NotUsed] = AvroParquetFlow(writer)
      val result =
        source
          .via(avroParquet)
          .runWith(Sink.seq)
      // #init-flow

      result.futureValue

      // then
      val parquetContent: List[GenericRecord] = fromParquet(file, conf)
      parquetContent.length shouldEqual n
      parquetContent should contain theSameElementsAs records
    }

    "insert avro records in parquet from a subtype of `GenericRecord`" in assertAllStagesStopped {
      // given
      val n: Int = 2
      val file: String = genFinalFile.sample.get
      val documents: List[Document] = genDocuments(n).sample.get
      val avroDocuments: List[Record] = documents.map(format.to)
      val writer: ParquetWriter[Record] = parquetWriter[Record](file, conf, schema)

      // when
      Source(avroDocuments)
        .via(AvroParquetFlow[Record](writer))
        .runWith(Sink.seq)
        .futureValue

      // then
      val parquetContent: List[GenericRecord] = fromParquet(file, conf)
      parquetContent.length shouldEqual n
      parquetContent.map(format.from(_)) should contain theSameElementsAs documents
    }
  }

}
