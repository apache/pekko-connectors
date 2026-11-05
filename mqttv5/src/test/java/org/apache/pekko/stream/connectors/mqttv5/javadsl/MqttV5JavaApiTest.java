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

package org.apache.pekko.stream.connectors.mqttv5.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.pekko.Done;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings;
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage;
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS;
import org.apache.pekko.util.ByteString;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the MQTT settings and messages without a broker. */
public class MqttV5JavaApiTest {

  private static final MqttMessage MESSAGE =
      MqttMessage.create("topic", ByteString.fromString("payload"));

  @Test
  public void messageQos() {
    assertEquals(Optional.empty(), MESSAGE.getQoS());
    assertEquals(
        Optional.of(MqttQoS.atLeastOnce()), MESSAGE.withQos(MqttQoS.atLeastOnce()).getQoS());
  }

  @Test
  public void enhancedAuthFromJavaLambda() {
    MqttConnectionSettings settings =
        MqttConnectionSettings.create("tcp://localhost:1883", "client", new MemoryPersistence())
            .withEnhancedAuth(
                "method",
                new byte[] {1},
                (Integer reasonCode, MqttProperties properties) ->
                    Pair.create(reasonCode + 1, properties));
    assertEquals("client", settings.clientId());
  }

  @Test
  public void connectionSettingsGetters() {
    MqttConnectionSettings settings =
        MqttConnectionSettings.create("tcp://localhost:1883", "client", new MemoryPersistence())
            .withWill(MESSAGE)
            .withKeepAliveInterval(Duration.ofSeconds(1))
            .withConnectionTimeout(Duration.ofSeconds(2))
            .withServerUris(Arrays.asList("tcp://a:1883", "tcp://b:1883"))
            .withSslProperties(Collections.singletonMap("k", "v"))
            .withOfflinePersistenceSettings();

    assertEquals(Optional.empty(), settings.getSocketFactory());
    assertEquals(Optional.of(MESSAGE), settings.getWill());
    assertEquals(Duration.ofSeconds(1), settings.getKeepAliveInterval());
    assertEquals(Duration.ofSeconds(2), settings.getConnectionTimeout());
    assertEquals(Arrays.asList("tcp://a:1883", "tcp://b:1883"), settings.getServerUris());
    assertEquals(Optional.empty(), settings.getSslHostnameVerifier());
    assertEquals(Collections.singletonMap("k", "v"), settings.getSslProperties());
  }

  @Test
  public void messageWithAckFactory() {
    MqttMessageWithAck message =
        MqttMessageWithAckImpl.create(
            MESSAGE, () -> CompletableFuture.completedFuture(Done.getInstance()));
    assertSame(MESSAGE, message.message());
    assertEquals(Done.getInstance(), message.ack().toCompletableFuture().join());
  }
}
