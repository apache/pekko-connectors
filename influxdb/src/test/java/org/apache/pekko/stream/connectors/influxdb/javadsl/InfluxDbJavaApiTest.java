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

package org.apache.pekko.stream.connectors.influxdb.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.influxdb.InfluxDbReadSettings;
import org.apache.pekko.stream.connectors.influxdb.InfluxDbWriteMessage;
import org.apache.pekko.stream.connectors.influxdb.InfluxDbWriteResult;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the InfluxDB model without an InfluxDB server. */
public class InfluxDbJavaApiTest {

  @Test
  public void modelGetters() {
    InfluxDbWriteMessage<String, NotUsed> message =
        InfluxDbWriteMessage.create("point").withDatabaseName("db").withRetentionPolicy("policy");
    assertEquals(Optional.of("db"), message.getDatabaseName());
    assertEquals(Optional.of("policy"), message.getRetentionPolicy());

    InfluxDbWriteResult<String, NotUsed> result =
        new InfluxDbWriteResult<>(message, scala.Option.apply("boom"));
    assertEquals(Optional.of("boom"), result.getError());

    assertSame(InfluxDbReadSettings.Default(), InfluxDbReadSettings.create());
  }
}
