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

package org.apache.pekko.stream.connectors.kudu.javadsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.PartialRow;
import org.apache.pekko.stream.connectors.kudu.KuduTableSettings;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Kudu settings without a Kudu cluster. */
public class KuduJavaApiTest {

  @Test
  public void settingsFromJavaFunctions() {
    Schema schema =
        new Schema(
            Collections.singletonList(
                new ColumnSchema.ColumnSchemaBuilder("key", Type.INT32).key(true).build()));
    KuduTableSettings<Integer> settings =
        KuduTableSettings.create(
                "table",
                schema,
                new CreateTableOptions(),
                (Integer value) -> {
                  PartialRow row = schema.newPartialRow();
                  row.addInt(0, value);
                  return row;
                })
            .withJavaConverter(
                (Integer value) -> {
                  PartialRow row = schema.newPartialRow();
                  row.addInt(0, value + 1);
                  return row;
                });

    PartialRow row = settings.getConverter().apply(1);
    assertNotNull(row);
  }
}
