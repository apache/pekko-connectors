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

package docs.scaladsl

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.actor.ActorSystem
import pekko.stream.connectors.avroparquet.scaladsl.AvroParquetSink
import pekko.stream.scaladsl.Source
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.testkit.TestKit
import com.sksamuel.avro4s.{ Record, RecordFormat }
import org.scalatest.concurrent.ScalaFutures
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.hadoop.ParquetWriter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class AvroParquetSinkSpec
    extends TestKit(ActorSystem("SinkSpec"))
    with AnyWordSpecLike
    with Matchers
    with AbstractAvroParquet
    with ScalaFutures
    with BeforeAndAfterAll {

  "Parquet Sink" should {

    "create new parquet file from `GenericRecords`" in assertAllStagesStopped {
      // given
      val n: Int = 3
      val file: String = genFinalFile.sample.get
      val records: List[GenericRecord] = genDocuments(n).sample.get.map(docToGenericRecord)

      Source(records).runWith(AvroParquetSink(parquetWriter(file, conf, schema))).futureValue

      // when
      val parquetContent: List[GenericRecord] = fromParquet(file, conf)

      // then
      parquetContent.length shouldEqual n
      parquetContent should contain theSameElementsAs records
    }

    "create new parquet file from any subtype of `GenericRecord` " in assertAllStagesStopped {
      // given
      val n: Int = 3
      val file: String = genFinalFile.sample.get
      val documents: List[Document] = genDocuments(n).sample.get
      val writer: ParquetWriter[Record] = parquetWriter[Record](file, conf, schema)
      // #init-sink
      val records: List[Record] = documents.map(RecordFormat[Document].to(_))
      val source: Source[Record, NotUsed] = Source(records)
      val result: Future[Done] = source
        .runWith(AvroParquetSink(writer))
      // #init-sink
      result.futureValue shouldBe Done

      // when
      val parquetContent: List[GenericRecord] = fromParquet(file, conf)

      // then
      parquetContent.length shouldEqual n
      parquetContent.map(format.from(_)) should contain theSameElementsAs documents
    }

  }

}
