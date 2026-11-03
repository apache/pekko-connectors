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

package org.apache.pekko.stream.connectors.amqp.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.net.ssl.SSLContext;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.amqp.*;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the AMQP settings and model classes without a broker. */
public class AmqpJavaApiTest {

  private static final Map<String, Object> ARGUMENTS =
      Collections.singletonMap("x-message-ttl", (Object) 1000);

  @Test
  public void hostAndPortPairsUseJavaIntegers() {
    List<Pair<String, Integer>> hostsAndPorts =
        Arrays.asList(Pair.create("host1", 5672), Pair.create("host2", 5673));

    AmqpDetailsConnectionProvider details = AmqpDetailsConnectionProvider.create(hostsAndPorts);
    assertEquals(hostsAndPorts, details.getHostAndPortList());
    assertEquals(
        hostsAndPorts,
        AmqpDetailsConnectionProvider.create("other", 1)
            .withHostsAndPorts(hostsAndPorts)
            .getHostAndPortList());

    AmqpConnectionFactoryConnectionProvider factoryProvider =
        AmqpConnectionFactoryConnectionProvider.create(new ConnectionFactory())
            .withHostsAndPorts(hostsAndPorts);
    assertEquals(hostsAndPorts, factoryProvider.getHostAndPortList());
  }

  @Test
  public void detailsConnectionProviderGetters() {
    AmqpCredentials credentials = AmqpCredentials.create("user", "pass");
    AmqpDetailsConnectionProvider provider =
        AmqpDetailsConnectionProvider.create("localhost", 5672)
            .withCredentials(credentials)
            .withVirtualHost("/vhost")
            .withRequestedHeartbeat(10)
            .withConnectionTimeout(20)
            .withHandshakeTimeout(30)
            .withShutdownTimeout(40)
            .withNetworkRecoveryInterval(50)
            .withConnectionName("connection");

    assertEquals(Optional.of(credentials), provider.getCredentials());
    assertEquals(Optional.of("/vhost"), provider.getVirtualHost());
    assertEquals(Optional.empty(), provider.getSslConfiguration());
    assertEquals(Optional.of(10), provider.getRequestedHeartbeat());
    assertEquals(Optional.of(20), provider.getConnectionTimeout());
    assertEquals(Optional.of(30), provider.getHandshakeTimeout());
    assertEquals(Optional.of(40), provider.getShutdownTimeout());
    assertEquals(Optional.of(50), provider.getNetworkRecoveryInterval());
    assertEquals(Optional.empty(), provider.getExceptionHandler());
    assertEquals(Optional.of("connection"), provider.getConnectionName());
  }

  @Test
  public void sslConfigurationTakesAnSslContextDirectly() throws NoSuchAlgorithmException {
    SSLContext context = SSLContext.getDefault();

    AmqpSSLConfiguration configuration = AmqpSSLConfiguration.create().withSSLContext(context);
    assertEquals(Optional.of(context), configuration.getContext());
    assertEquals(Optional.empty(), configuration.getProtocol());
    assertEquals(Optional.empty(), configuration.getTrustManager());

    assertEquals(
        Optional.empty(), configuration.withSSLContext(Optional.<SSLContext>empty()).getContext());
  }

  @Test
  public void writeMessageGetters() {
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().build();
    WriteMessage message =
        WriteMessage.create(ByteString.fromString("hello"))
            .withProperties(properties)
            .withRoutingKey("key");

    assertEquals(Optional.of(properties), message.getProperties());
    assertEquals(Optional.of("key"), message.getRoutingKey());
    assertEquals(
        Optional.empty(), WriteMessage.create(ByteString.emptyByteString()).getRoutingKey());
  }

  @Test
  public void sourceSettingsGetters() {
    QueueDeclaration queue = QueueDeclaration.create("queue");
    NamedQueueSourceSettings named =
        NamedQueueSourceSettings.create(AmqpLocalConnectionProvider.getInstance(), "queue")
            .withDeclarations(Collections.singletonList(queue))
            .withArguments(ARGUMENTS);
    assertEquals(Collections.singletonList(queue), named.getDeclarations());
    assertEquals(ARGUMENTS, named.getArguments());

    TemporaryQueueSourceSettings temporary =
        TemporaryQueueSourceSettings.create(AmqpLocalConnectionProvider.getInstance(), "exchange")
            .withDeclaration(queue)
            .withRoutingKey("key");
    assertEquals(Collections.singletonList(queue), temporary.getDeclarations());
    assertEquals(Optional.of("key"), temporary.getRoutingKey());
  }

  @Test
  public void writeSettingsGetters() {
    QueueDeclaration queue = QueueDeclaration.create("queue");
    AmqpWriteSettings settings =
        AmqpWriteSettings.create(AmqpLocalConnectionProvider.getInstance())
            .withExchange("exchange")
            .withRoutingKey("key")
            .withDeclaration(queue)
            .withConfirmationTimeout(Duration.ofSeconds(3));

    assertEquals(Optional.of("exchange"), settings.getExchange());
    assertEquals(Optional.of("key"), settings.getRoutingKey());
    assertEquals(Collections.singletonList(queue), settings.getDeclarations());
    assertEquals(Duration.ofSeconds(3), settings.getConfirmationTimeout());
    assertEquals(
        Collections.emptyList(),
        AmqpReplyToSinkSettings.create(AmqpLocalConnectionProvider.getInstance())
            .getDeclarations());
  }

  @Test
  public void declarationGetters() {
    assertEquals(
        ARGUMENTS, QueueDeclaration.create("queue").withArguments(ARGUMENTS).getArguments());
    assertEquals(
        ARGUMENTS,
        ExchangeDeclaration.create("exchange", "topic").withArguments(ARGUMENTS).getArguments());

    BindingDeclaration binding =
        BindingDeclaration.create("queue", "exchange")
            .withRoutingKey("key")
            .withArguments(ARGUMENTS);
    assertEquals(Optional.of("key"), binding.getRoutingKey());
    assertEquals(ARGUMENTS, binding.getArguments());
    assertFalse(BindingDeclaration.create("queue", "exchange").getRoutingKey().isPresent());
  }

  @Test
  public void rpcFlowOverloadsWithoutRepliesPerMessage() {
    AmqpWriteSettings settings =
        AmqpWriteSettings.create(AmqpLocalConnectionProvider.getInstance());

    Flow<ByteString, ByteString, CompletionStage<String>> simple =
        AmqpRpcFlow.createSimple(settings);
    Flow<WriteMessage, CommittableReadResult, CompletionStage<String>> committable =
        AmqpRpcFlow.committableFlow(settings, 10);

    assertNotNull(simple);
    assertNotNull(committable);
  }
}
