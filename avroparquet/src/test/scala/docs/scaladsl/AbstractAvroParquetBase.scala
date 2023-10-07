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

import org.apache.avro.Schema
import org.apache.avro.generic.{ GenericRecord, GenericRecordBuilder }
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.{ AvroParquetReader, AvroParquetWriter, AvroReadSupport }
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ ParquetReader, ParquetWriter }
import org.scalacheck.Gen

import scala.util.Random

trait AbstractAvroParquetBase {
  case class Document(id: String, body: String)

  val schema: Schema = new Schema.Parser().parse(
    "{\"type\":\"record\",\"name\":\"Document\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"body\",\"type\":\"string\"}]}")

  val genDocument: Gen[Document] =
    Gen.oneOf(Seq(Document(id = Gen.alphaStr.sample.get, body = Gen.alphaLowerStr.sample.get)))
  val genDocuments: Int => Gen[List[Document]] = n => Gen.listOfN(n, genDocument)

  val folder: String = "./" + Random.alphanumeric.take(8).mkString("")

  val genFinalFile: Gen[String] = for {
    fileName <- Gen.alphaLowerStr
  } yield {
    folder + "/" + fileName + ".parquet"
  }

  val genFile: Gen[String] = Gen.oneOf(Seq(Gen.alphaLowerStr.sample.get + ".parquet"))

  val conf: Configuration = new Configuration()
  conf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, true)

  @SuppressWarnings(Array("deprecation"))
  def parquetWriter[T <: GenericRecord](file: String, conf: Configuration, schema: Schema): ParquetWriter[T] =
    AvroParquetWriter.builder[T](new Path(file)).withConf(conf).withSchema(schema).build()

  def parquetReader[T <: GenericRecord](file: String, conf: Configuration): ParquetReader[T] =
    AvroParquetReader.builder[T](HadoopInputFile.fromPath(new Path(file), conf)).withConf(conf).build()

  def docToGenericRecord(document: Document): GenericRecord =
    new GenericRecordBuilder(schema)
      .set("id", document.id)
      .set("body", document.body)
      .build()

  def fromParquet(file: String, configuration: Configuration): List[GenericRecord] = {
    val reader = parquetReader[GenericRecord](file, conf)
    var record: GenericRecord = reader.read()
    var result: List[GenericRecord] = List.empty[GenericRecord]
    while (record != null) {
      result = result ::: record :: Nil
      record = reader.read()
    }
    result
  }

  def sourceDocumentation(): Unit = {
    // #prepare-source
    import org.apache.hadoop.conf.Configuration
    import org.apache.parquet.avro.AvroReadSupport

    val conf: Configuration = new Configuration()
    conf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, true)
    // #prepare-source
  }

  @SuppressWarnings(Array("deprecation"))
  def sinkDocumentation(): Unit = {
    // #prepare-sink
    import com.sksamuel.avro4s.Record
    import org.apache.hadoop.conf.Configuration
    import org.apache.hadoop.fs.Path
    import org.apache.parquet.avro.AvroReadSupport

    val file = "./sample/path/test.parquet"
    val conf = new Configuration()
    conf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, true)
    val writer =
      AvroParquetWriter.builder[Record](new Path(file)).withConf(conf).withSchema(schema).build()
    // #prepare-sink
    if (writer != null) { // forces val usage
    }
  }

  @SuppressWarnings(Array("deprecation"))
  def initWriterDocumentation(): Unit = {
    // #init-writer
    import org.apache.avro.generic.GenericRecord
    import org.apache.hadoop.fs.Path
    import org.apache.parquet.avro.AvroParquetReader
    import org.apache.parquet.hadoop.util.HadoopInputFile

    val file = "./sample/path/test.parquet"
    val writer =
      AvroParquetWriter.builder[GenericRecord](new Path(file)).withConf(conf).withSchema(schema).build()
    // #init-writer
    // #init-reader
    val reader =
      AvroParquetReader.builder[GenericRecord](HadoopInputFile.fromPath(new Path(file), conf)).withConf(conf).build()
    // #init-reader
    if (writer != null && reader != null) { // forces val usage
    }
  }
}
