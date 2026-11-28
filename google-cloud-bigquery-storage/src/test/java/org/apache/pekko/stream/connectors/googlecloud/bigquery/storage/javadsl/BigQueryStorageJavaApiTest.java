/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.cloud.bigquery.storage.v1.DataFormat;
import com.google.cloud.bigquery.storage.v1.ReadSession;
import com.google.cloud.bigquery.storage.v1.arrow.ArrowRecordBatch;
import com.google.cloud.bigquery.storage.v1.arrow.ArrowSchema;
import com.google.cloud.bigquery.storage.v1.avro.AvroRows;
import com.google.cloud.bigquery.storage.v1.avro.AvroSchema;
import com.google.cloud.bigquery.storage.v1.storage.ReadRowsResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.NotUsed;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the BigQuery Storage connector without BigQuery. */
public class BigQueryStorageJavaApiTest {

  @Test
  public void recordGetOptional() {
    BigQueryRecord record =
        column ->
            "present".equals(column) ? scala.Option.apply((Object) "value") : scala.Option.empty();
    assertEquals(Optional.of("value"), record.getOptional("present"));
    assertEquals(Optional.empty(), record.getOptional("absent"));
  }

  /** Compile-only check: reads emit Pair and take the Java protobuf read options. */
  @SuppressWarnings("unused")
  private static void readsCompile(ReadSession.TableReadOptions readOptions) {
    Source<
            Pair<
                com.google.cloud.bigquery.storage.v1.stream.ReadSession.Schema,
                List<Source<ReadRowsResponse.Rows, NotUsed>>>,
            CompletionStage<NotUsed>>
        rows = BigQueryStorage.createAsPair("p", "d", "t", DataFormat.AVRO);
    Source<Pair<ArrowSchema, Source<ArrowRecordBatch, NotUsed>>, CompletionStage<NotUsed>> arrow =
        BigQueryArrowStorage.readMergedAsPair("p", "d", "t", readOptions);
    Source<Pair<ArrowSchema, List<Source<ArrowRecordBatch, NotUsed>>>, CompletionStage<NotUsed>>
        arrowStreams = BigQueryArrowStorage.readAsPair("p", "d", "t", readOptions, 1);
    Source<Pair<AvroSchema, Source<AvroRows, NotUsed>>, CompletionStage<NotUsed>> avro =
        BigQueryAvroStorage.readMergedAsPair("p", "d", "t");
    Source<List<BigQueryRecord>, CompletionStage<NotUsed>> records =
        BigQueryAvroStorage.readRecordsMerged("p", "d", "t", readOptions);
  }
}
