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

package org.apache.pekko.stream.connectors.elasticsearch.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.http.javadsl.model.headers.RawHeader;
import org.apache.pekko.stream.connectors.elasticsearch.*;
import org.apache.pekko.stream.connectors.elasticsearch.testkit.MessageFactory;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Elasticsearch model and settings without a cluster. */
public class ElasticsearchJavaApiTest {

  @Test
  public void readResultVersionIsAJavaLong() {
    ReadResult<String> result = MessageFactory.createReadResult("id", "source", Optional.of(5L));
    Optional<Long> version = result.getVersion();
    assertEquals(Optional.of(5L), version);
  }

  @Test
  public void writeMessageGetters() {
    Map<String, String> metadata = Collections.singletonMap("routing", "r1");
    WriteMessage<String, NotUsed> message =
        WriteMessage.createIndexMessage("id", "source")
            .withVersion(3L)
            .withIndexName("index")
            .withCustomMetadata(metadata);

    assertEquals(Optional.of("id"), message.getId());
    assertEquals(Optional.of("source"), message.getSource());
    assertEquals(Optional.of(3L), message.getVersion());
    assertEquals(Optional.of("index"), message.getIndexName());
    assertEquals(metadata, message.getCustomMetadata());
  }

  @Test
  public void retryLogicFromJava() {
    RetryLogic retry =
        new AbstractRetryLogic() {
          @Override
          public int maxRetries() {
            return 3;
          }

          @Override
          public Duration getMinBackoff() {
            return Duration.ofMillis(100);
          }

          @Override
          public Duration getMaxBackoff() {
            return Duration.ofSeconds(2);
          }
        };
    assertEquals(3, retry.maxRetries());
    assertEquals(100, retry.minBackoff().toMillis());

    RetryLogic never = RetryNever.getInstance();
    assertEquals(0, never.maxRetries());
  }

  @Test
  public void settingsGetters() {
    ElasticsearchConnectionSettings connection =
        ElasticsearchConnectionSettings.create("http://localhost:9200")
            .withCredentials("user", "pass")
            .withHeaders(Collections.singletonList(RawHeader.create("X-Test", "1")));
    assertEquals(Optional.of("user"), connection.getUsername());
    assertEquals(Optional.of("pass"), connection.getPassword());
    assertEquals("X-Test", connection.getHeaders().get(0).name());
    assertEquals(Optional.empty(), connection.getConnectionContext());

    ElasticsearchWriteSettings write =
        ElasticsearchWriteSettings.create(connection).withVersionType("external");
    assertEquals(Optional.of("external"), write.getVersionType());

    ElasticsearchSourceSettings source =
        ElasticsearchSourceSettings.create(connection).withScrollDuration(Duration.ofMinutes(2));
    assertEquals(Duration.ofMinutes(2), source.getScrollDuration());

    assertEquals(Optional.empty(), ElasticsearchParams.V8("index").getTypeName());
  }

  /** Compile-only check: the sink and sources have Java-friendly overloads. */
  @SuppressWarnings("unused")
  private static void stagesCompile(
      ElasticsearchConnectionSettings connection, MessageWriter<String> writer) {
    ElasticsearchParams params = ElasticsearchParams.V8("index");
    Sink<WriteMessage<String, NotUsed>, java.util.concurrent.CompletionStage<Done>> sink =
        ElasticsearchSink.create(params, ElasticsearchWriteSettings.create(connection), writer);
    Map<String, String> searchParams = Collections.singletonMap("query", "{\"match_all\": {}}");
    Source<ReadResult<Map<String, Object>>, NotUsed> source =
        ElasticsearchSource.create(
            params, searchParams, ElasticsearchSourceSettings.create(connection));
    Source<ReadResult<String>, NotUsed> typed =
        ElasticsearchSource.typed(
            params, searchParams, ElasticsearchSourceSettings.create(connection), String.class);
  }
}
