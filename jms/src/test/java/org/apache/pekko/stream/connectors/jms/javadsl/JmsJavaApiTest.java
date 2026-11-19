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

package org.apache.pekko.stream.connectors.jms.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.jms.*;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the JMS settings and messages without a broker. */
public class JmsJavaApiTest {

  private static final ConnectionFactory CONNECTION_FACTORY =
      new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");

  @Test
  public void customDestinationFromJavaLambda() {
    CustomDestination destination =
        CustomDestination.fromFunction(
            "custom",
            session -> {
              try {
                return session.createQueue("queue");
              } catch (javax.jms.JMSException e) {
                throw new RuntimeException(e);
              }
            });
    assertEquals("custom", destination.name());
  }

  @Test
  public void timeToLiveFromJavaDuration() {
    assertEquals(
        JmsTimeToLive.create(2, java.util.concurrent.TimeUnit.SECONDS),
        JmsTimeToLive.create(Duration.ofSeconds(2)));
  }

  @Test
  public void messagesTakeJavaCollections() {
    Set<JmsHeader> headers = new HashSet<>();
    headers.add(JmsType.create("type"));
    headers.add(JmsCorrelationId.create("correlation"));

    JmsTextMessage text = JmsTextMessage.create("body").withHeaders(headers);
    assertTrue(text.getHeaders().containsAll(headers));

    JmsMessage message = JmsByteMessage.create(new byte[] {1}).withHeaders(headers);
    assertTrue(message.getHeaders().containsAll(headers));

    Set<JmsType> subtypeHeaders = Collections.singleton(JmsType.create("subtype"));
    assertTrue(
        JmsTextMessage.create("body")
            .withHeaders(subtypeHeaders)
            .getHeaders()
            .containsAll(subtypeHeaders));

    Map<String, Object> properties = Collections.singletonMap("key", "value");
    JmsObjectMessagePassThrough<String> object =
        JmsObjectMessage.create("serializable", "passThrough").withProperties(properties);
    assertEquals("value", object.getProperties().get("key"));

    JmsEnvelopeWithProperties<String> withProperties = object;
    assertEquals("value", withProperties.getProperties().get("key"));

    Map<String, Object> body = Collections.singletonMap("field", 1);
    JmsMapMessagePassThrough<NotUsed> map = JmsMapMessage.create(body);
    assertEquals(body, map.getBody());
  }

  @Test
  public void consumerSettingsGetters() {
    Credentials credentials = Credentials.create("user", "pass");
    JmsConsumerSettings settings =
        JmsConsumerSettings.create(defaultConsumerConfig(), CONNECTION_FACTORY)
            .withQueue("queue")
            .withCredentials(credentials)
            .withSelector("a = 1")
            .withAcknowledgeMode(AcknowledgeMode.ClientAcknowledge())
            .withAckTimeout(Duration.ofSeconds(3))
            .withMaxAckInterval(Duration.ofSeconds(4))
            .withConnectionStatusSubscriptionTimeout(Duration.ofSeconds(5));

    assertEquals(Optional.of(new Queue("queue")), settings.getDestination());
    assertEquals(Optional.of(credentials), settings.getCredentials());
    assertEquals(Optional.of("a = 1"), settings.getSelector());
    assertEquals(Optional.of(AcknowledgeMode.ClientAcknowledge()), settings.getAcknowledgeMode());
    assertEquals(Duration.ofSeconds(3), settings.getAckTimeout());
    assertEquals(Optional.of(Duration.ofSeconds(4)), settings.getMaxAckInterval());
    assertEquals(Duration.ofSeconds(5), settings.getConnectionStatusSubscriptionTimeout());
    assertEquals(
        ChronoUnit.FOREVER.getDuration(),
        settings.withAckTimeout(scala.concurrent.duration.Duration.Inf()).getAckTimeout());
  }

  @Test
  public void producerSettingsGetters() {
    JmsProducerSettings settings =
        JmsProducerSettings.create(defaultProducerConfig(), CONNECTION_FACTORY)
            .withTopic("topic")
            .withTimeToLive(Duration.ofSeconds(6))
            .withConnectionStatusSubscriptionTimeout(Duration.ofSeconds(7));

    assertEquals(Optional.of(new Topic("topic")), settings.getDestination());
    assertEquals(Optional.empty(), settings.getCredentials());
    assertEquals(Optional.of(Duration.ofSeconds(6)), settings.getTimeToLive());
    assertEquals(Duration.ofSeconds(7), settings.getConnectionStatusSubscriptionTimeout());
  }

  @Test
  public void browseAndRetrySettingsGetters() {
    JmsBrowseSettings browse =
        JmsBrowseSettings.create(defaultBrowseConfig(), CONNECTION_FACTORY)
            .withQueue("queue")
            .withSelector("b = 2");
    assertEquals(Optional.of(new Queue("queue")), browse.getDestination());
    assertEquals(Optional.empty(), browse.getCredentials());
    assertEquals(Optional.of("b = 2"), browse.getSelector());

    ConnectionRetrySettings connectionRetry =
        ConnectionRetrySettings.create(defaultConnectionRetryConfig())
            .withConnectTimeout(Duration.ofSeconds(1))
            .withInitialRetry(Duration.ofSeconds(2))
            .withMaxBackoff(Duration.ofSeconds(3));
    assertEquals(Duration.ofSeconds(1), connectionRetry.getConnectTimeout());
    assertEquals(Duration.ofSeconds(2), connectionRetry.getInitialRetry());
    assertEquals(Duration.ofSeconds(3), connectionRetry.getMaxBackoff());

    SendRetrySettings sendRetry =
        SendRetrySettings.create(defaultSendRetryConfig())
            .withInitialRetry(Duration.ofSeconds(4))
            .withMaxBackoff(Duration.ofSeconds(5));
    assertEquals(Duration.ofSeconds(4), sendRetry.getInitialRetry());
    assertEquals(Duration.ofSeconds(5), sendRetry.getMaxBackoff());
  }

  private static com.typesafe.config.Config reference(String path) {
    return com.typesafe.config.ConfigFactory.load().getConfig(path);
  }

  private static com.typesafe.config.Config defaultConsumerConfig() {
    return reference(JmsConsumerSettings.configPath());
  }

  private static com.typesafe.config.Config defaultProducerConfig() {
    return reference(JmsProducerSettings.configPath());
  }

  private static com.typesafe.config.Config defaultBrowseConfig() {
    return reference(JmsBrowseSettings.configPath());
  }

  private static com.typesafe.config.Config defaultConnectionRetryConfig() {
    return reference(ConnectionRetrySettings.configPath());
  }

  private static com.typesafe.config.Config defaultSendRetryConfig() {
    return reference(SendRetrySettings.configPath());
  }
}
