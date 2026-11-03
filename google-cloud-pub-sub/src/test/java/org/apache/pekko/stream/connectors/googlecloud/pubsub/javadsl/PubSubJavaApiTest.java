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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.*;
import org.apache.pekko.stream.javadsl.Flow;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Pub/Sub model without Pub/Sub. */
public class PubSubJavaApiTest {

  private static final Map<String, String> ATTRIBUTES = Collections.singletonMap("k", "v");

  @Test
  public void pubSubMessageGetters() {
    PubSubMessage message =
        PubSubMessage.create(Optional.of("data"), Optional.of(ATTRIBUTES), "id", Instant.EPOCH);
    assertEquals(Optional.of("data"), message.getData());
    assertEquals(Optional.of(ATTRIBUTES), message.getAttributes());
    assertEquals(Optional.empty(), message.getOrderingKey());
  }

  @Test
  public void requestsGetters() {
    PublishMessage message = PublishMessage.create("data", ATTRIBUTES);
    assertEquals(Optional.of(ATTRIBUTES), message.getAttributes());
    assertEquals(
        Collections.singletonList(message),
        PublishRequest.create(Collections.singletonList(message)).getMessages());
    assertEquals(
        Collections.singletonList("ack"),
        AcknowledgeRequest.create(Collections.singletonList("ack")).getAckIds());
  }

  /** Compile-only check: publish doesn't require parallelism. */
  @SuppressWarnings("unused")
  private static void publishCompiles(PubSubConfig config) {
    Flow<PublishRequest, List<String>, NotUsed> publish = GooglePubSub.publish("topic", config);
    Flow<PublishRequest, List<String>, NotUsed> publishHost =
        GooglePubSub.publish("topic", config, "host");
  }
}
