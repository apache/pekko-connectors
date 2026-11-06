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

package org.apache.pekko.stream.connectors.hbase.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.pekko.stream.connectors.hbase.HTableSettings;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the HBase settings without an HBase cluster. */
public class HBaseJavaApiTest {

  @Test
  public void settingsFromJavaLambdas() {
    HTableSettings<String> settings =
        HTableSettings.<String>create(
                new Configuration(),
                TableName.valueOf("table"),
                Arrays.asList("family1", "family2"),
                value -> Collections.emptyList())
            .withJavaConverter(value -> Collections.singletonList(new Put(value.getBytes())));

    assertEquals(Arrays.asList("family1", "family2"), settings.getColumnFamilies());
    List<Mutation> mutations = settings.getConverter().apply("row");
    assertEquals(1, mutations.size());
  }
}
