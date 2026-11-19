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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineDistribution;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Pub/Sub gRPC settings without Pub/Sub. */
public class PubSubGrpcJavaApiTest {

  @Test
  public void settingsTls() {
    PubSubSettings settings =
        PubSubSettings.create("host", 443).withUseTls(true).withRootCa("ca.pem");
    assertTrue(settings.useTls());
    assertEquals(Optional.of("ca.pem"), settings.getRootCa());
  }

  @Test
  public void ackDeadlineDistributionFromDurations() {
    assertNotNull(
        AckDeadlineDistribution.create(
            Duration.ofSeconds(60),
            Duration.ofSeconds(10),
            Duration.ofSeconds(600),
            Duration.ofMinutes(60),
            0.99));
  }
}
