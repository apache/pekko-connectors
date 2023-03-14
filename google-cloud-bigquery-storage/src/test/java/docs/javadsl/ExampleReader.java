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

package docs.javadsl;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.ActorMaterializer;
// #read-all
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryStorageSettings;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl.BigQueryArrowStorage;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl.BigQueryAvroStorage;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl.BigQueryStorage;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.BigQueryStorageAttributes;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.GrpcBigQueryStorageReader;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import scala.Tuple2;
import com.google.cloud.bigquery.storage.v1.DataFormat;
import com.google.cloud.bigquery.storage.v1.ReadSession;
import com.google.cloud.bigquery.storage.v1.storage.ReadRowsResponse;
import org.apache.pekko.http.javadsl.unmarshalling.Unmarshaller;

// #read-all

public class ExampleReader {

  static final ActorSystem sys = ActorSystem.create("ExampleReader");
  static final ActorMaterializer mat = ActorMaterializer.create(sys);

  // #read-all
  Source<
          Tuple2<
              com.google.cloud.bigquery.storage.v1.stream.ReadSession.Schema,
              List<Source<ReadRowsResponse.Rows, NotUsed>>>,
          CompletionStage<NotUsed>>
      sourceOfSources =
          BigQueryStorage.create("projectId", "datasetId", "tableId", DataFormat.AVRO);
  // #read-all

  // #read-options
  ReadSession.TableReadOptions readOptions =
      ReadSession.TableReadOptions.newBuilder()
          .setSelectedFields(0, "stringField")
          .setSelectedFields(1, "intField")
          .setRowRestriction("intField >= 5")
          .build();

  Source<
          Tuple2<
              com.google.cloud.bigquery.storage.v1.stream.ReadSession.Schema,
              List<Source<ReadRowsResponse.Rows, NotUsed>>>,
          CompletionStage<NotUsed>>
      sourceOfSourcesFiltered =
          BigQueryStorage.create(
              "projectId", "datasetId", "tableId", DataFormat.AVRO, readOptions, 1);
  // #read-options

  // #read-merged
  Unmarshaller<ByteString, List<BigQueryRecord>> unmarshaller = null;
  Source<List<BigQueryRecord>, CompletionStage<NotUsed>> sequentialSource =
      BigQueryStorage.<List<BigQueryRecord>>createMergedStreams(
          "projectId", "datasetId", "tableId", DataFormat.AVRO, unmarshaller);
  // #read-merged

  // #read-arrow-merged
  Source<List<BigQueryRecord>, CompletionStage<NotUsed>> arrowSequentialSource =
      BigQueryArrowStorage.readRecordsMerged("projectId", "datasetId", "tableId");
  // #read-arrow-merged

  // #read-arrow-all
  Source<List<Source<BigQueryRecord, NotUsed>>, CompletionStage<NotUsed>> arrowParallelSource =
      BigQueryArrowStorage.readRecords("projectId", "datasetId", "tableId");
  // #read-arrow-all

  // #read-avro-merged
  Source<List<BigQueryRecord>, CompletionStage<NotUsed>> avroSequentialSource =
      BigQueryAvroStorage.readRecordsMerged("projectId", "datasetId", "tableId");
  // #read-avro-merged

  // #read-avro-all
  Source<List<Source<BigQueryRecord, NotUsed>>, CompletionStage<NotUsed>> avroParallelSource =
      BigQueryAvroStorage.readRecords("projectId", "datasetId", "tableId");
  // #read-avro-all

  // #attributes
  GrpcBigQueryStorageReader reader =
      GrpcBigQueryStorageReader.apply(BigQueryStorageSettings.apply("localhost", 8000), sys);

  Source<
          Tuple2<
              com.google.cloud.bigquery.storage.v1.stream.ReadSession.Schema,
              List<Source<ReadRowsResponse.Rows, NotUsed>>>,
          CompletionStage<NotUsed>>
      sourceForReader =
          BigQueryStorage.create(
                  "projectId", "datasetId", "tableId", DataFormat.AVRO, readOptions, 1)
              .withAttributes(BigQueryStorageAttributes.reader(reader));
  // #attributes
}
