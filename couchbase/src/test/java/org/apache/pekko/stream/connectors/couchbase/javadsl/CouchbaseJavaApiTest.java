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

package org.apache.pekko.stream.connectors.couchbase.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.connectors.couchbase.CouchbaseSessionSettings;
import org.apache.pekko.stream.connectors.couchbase.CouchbaseWriteSettings;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Couchbase settings without a Couchbase cluster. */
public class CouchbaseJavaApiTest {

  @Test
  public void sessionSettingsGetters() {
    CouchbaseSessionSettings settings =
        CouchbaseSessionSettings.create("user", "pass").withNodes(Arrays.asList("node1", "node2"));
    assertEquals(Arrays.asList("node1", "node2"), settings.getNodes());
    assertEquals(Optional.empty(), settings.getEnvironment());
  }

  @Test
  public void writeSettingsTimeout() {
    CouchbaseWriteSettings settings =
        CouchbaseWriteSettings.create().withTimeout(Duration.ofSeconds(5));
    assertEquals(Duration.ofSeconds(5), settings.getTimeout());
  }

  /** Compile-only check: varargs index fields and boxed result types are usable from Java. */
  @SuppressWarnings("unused")
  private static void sessionCompiles(CouchbaseSession session) {
    CompletionStage<Boolean> created = session.createIndex("index", true, "field1", "field2");
    CompletionStage<Long> counter = session.counter("id", 1L, 0L);
    CompletionStage<Long> counterWithSettings =
        session.counter("id", 1L, 0L, CouchbaseWriteSettings.create());
  }
}
