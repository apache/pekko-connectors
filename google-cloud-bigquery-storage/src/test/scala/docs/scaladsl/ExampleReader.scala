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

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import pekko.stream.connectors.googlecloud.bigquery.storage.{ BigQueryRecord, BigQueryStorageSettings }
import pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.{
  BigQueryArrowStorage,
  BigQueryAvroStorage,
  BigQueryStorageAttributes,
  GrpcBigQueryStorageReader
}
import org.scalatestplus.mockito.MockitoSugar.mock

//#read-all
import org.apache.pekko
import pekko.NotUsed
import com.google.cloud.bigquery.storage.v1.storage.ReadRowsResponse
import com.google.cloud.bigquery.storage.v1.DataFormat
import com.google.cloud.bigquery.storage.v1.stream.ReadSession
import pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.BigQueryStorage
import pekko.stream.scaladsl.Source
import com.google.cloud.bigquery.storage.v1.stream.ReadSession.TableReadOptions
import scala.concurrent.Future

//#read-all

class ExampleReader {

  implicit val sys: ActorSystem = ActorSystem("ExampleReader")

  // #read-all
  val sourceOfSources: Source[(ReadSession.Schema, Seq[Source[ReadRowsResponse.Rows, NotUsed]]), Future[NotUsed]] =
    BigQueryStorage.create("projectId", "datasetId", "tableId", DataFormat.AVRO)
  // #read-all

  // #read-options
  val readOptions = TableReadOptions(selectedFields = Seq("stringField", "intField"), rowRestriction = "intField >= 5")
  val sourceOfSourcesFiltered
      : Source[(ReadSession.Schema, Seq[Source[ReadRowsResponse.Rows, NotUsed]]), Future[NotUsed]] =
    BigQueryStorage.create("projectId", "datasetId", "tableId", DataFormat.AVRO, Some(readOptions))
  // #read-options

  // #read-merged
  implicit val unmarshaller: FromByteStringUnmarshaller[List[BigQueryRecord]] =
    mock[FromByteStringUnmarshaller[List[BigQueryRecord]]]
  val sequentialSource: Source[List[BigQueryRecord], Future[NotUsed]] =
    BigQueryStorage.createMergedStreams("projectId", "datasetId", "tableId", DataFormat.AVRO)
  // #read-merged

  // #read-arrow-merged
  val arrowSequentialSource: Source[Seq[BigQueryRecord], Future[NotUsed]] =
    BigQueryArrowStorage.readRecordsMerged("projectId", "datasetId", "tableId")
  // #read-arrow-merged

  // #read-arrow-all
  val arrowParallelSource: Source[Seq[Source[BigQueryRecord, NotUsed]], Future[NotUsed]] =
    BigQueryArrowStorage.readRecords("projectId", "datasetId", "tableId")
  // #read-arrow-all

  // #read-avro-merged
  val avroSequentialSource: Source[Seq[BigQueryRecord], Future[NotUsed]] =
    BigQueryAvroStorage.readRecordsMerged("projectId", "datasetId", "tableId")
  // #read-avro-merged

  // #read-avro-all
  val avroParallelSource: Source[Seq[Source[BigQueryRecord, NotUsed]], Future[NotUsed]] =
    BigQueryAvroStorage.readRecords("projectId", "datasetId", "tableId")
  // #read-avro-all

  // #attributes
  val reader: GrpcBigQueryStorageReader = GrpcBigQueryStorageReader(BigQueryStorageSettings("localhost", 8000))
  val sourceForReader: Source[(ReadSession.Schema, Seq[Source[ReadRowsResponse.Rows, NotUsed]]), Future[NotUsed]] =
    BigQueryStorage
      .create("projectId", "datasetId", "tableId", DataFormat.AVRO)
      .withAttributes(
        BigQueryStorageAttributes.reader(reader))
  // #attributes

}
