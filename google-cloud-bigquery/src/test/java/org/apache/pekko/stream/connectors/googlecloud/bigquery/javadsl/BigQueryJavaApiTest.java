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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.Dataset;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.JobConfiguration;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.QueryRequest;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableListResponse;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the BigQuery model without BigQuery. */
public class BigQueryJavaApiTest {

  private static final Map<String, String> LABELS = Collections.singletonMap("k", "v");

  @Test
  public void queryRequestGetters() {
    QueryRequest request =
        QueryRequest.create(
                "SELECT 1",
                OptionalInt.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty())
            .withLabels(Optional.of(LABELS))
            .withMaximumBytesBilled(java.util.OptionalLong.of(5L));
    Optional<Long> maximumBytesBilled = request.getMaximumBytesBilled();
    assertEquals(Optional.of(5L), maximumBytesBilled);
    Optional<Map<String, String>> labels = request.getLabelsAsJava();
    assertEquals(Optional.of(LABELS), labels);
  }

  /** The deprecated getter still returns a Scala Map, as callers compiled against 1.x expect. */
  @Test
  @SuppressWarnings("deprecation")
  public void queryRequestDeprecatedGetLabelsReturnsScalaMap() {
    QueryRequest request =
        QueryRequest.create(
                "SELECT 1",
                OptionalInt.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty())
            .withLabels(Optional.of(LABELS));
    Optional<scala.collection.immutable.Map<String, String>> labels = request.getLabels();
    assertEquals(scala.Option.apply("v"), labels.get().get("k"));
  }

  @Test
  public void jobConfigurationLabels() {
    JobConfiguration configuration =
        JobConfiguration.create(Optional.empty()).withLabels(Optional.of(LABELS));
    Optional<Map<String, String>> labels = configuration.getLabelsAsJava();
    assertEquals(Optional.of(LABELS), labels);
    assertThrows(UnsupportedOperationException.class, () -> labels.get().put("x", "y"));
  }

  /** The deprecated getter still returns a Scala Map, as callers compiled against 1.x expect. */
  @Test
  @SuppressWarnings("deprecation")
  public void jobConfigurationDeprecatedGetLabelsReturnsScalaMap() {
    JobConfiguration configuration =
        JobConfiguration.create(Optional.empty()).withLabels(Optional.of(LABELS));
    Optional<scala.collection.immutable.Map<String, String>> labels = configuration.getLabels();
    assertEquals(scala.Option.apply("v"), labels.get().get("k"));
  }

  @Test
  public void tableListResponseCreate() {
    TableListResponse response =
        TableListResponse.create(Optional.of("token"), Optional.empty(), OptionalInt.empty());
    assertEquals(Optional.of("token"), response.getNextPageToken());
  }

  /** Compile-only check: datasets can be listed without arguments. */
  @SuppressWarnings("unused")
  private static void listDatasetsCompiles() {
    Source<Dataset, NotUsed> datasets = BigQuery.listDatasets();
  }
}
