/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.amqp.javadsl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.pekko.stream.connectors.amqp.*;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;

@ExtendWith(LogCapturingExtension.class)
public class AmqpConnectionProvidersTest {

  @Test
  public void LocalAmqpConnectionCreatesNewConnection() throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void LocalAmqpConnectionReleaseClosedConnectionDoNotError() throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void AmqpConnectionUriCreatesNewConnection() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void AmqpConnectionUriReleaseClosedConnectionDoNotError() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void AmqpConnectionDetailsCreatesNewConnection() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void AmqpConnectionDetailsReleaseClosedConnectionDoNotError() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void AmqpConnectionFactoryCreatesNewConnection() throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void AmqpConnectionFactoryReleaseClosedConnectionDoNotError() throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndLocalAmqpConnectionReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndAmqpConnectionUriReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndAmqpConnectionDetailsReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndmqpConnectionFactoryReusesConnection()
          throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndLocalAmqpConnectionReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndAmqpConnectionUriReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndAmqpConnectionDetailsReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndAmqpConnectionFactoryReusesConnection()
          throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void ReusableAMQPConnectionProviderNeverLeftInInvalidStateUponException()
      throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5673);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    try {
      reusableConnectionProvider.get();
    } catch (Exception e) {
      assertThat(e, instanceOf(ConnectException.class));
    }

    try {
      reusableConnectionProvider.get();
    } catch (Exception e) {
      assertThat(e, instanceOf(ConnectException.class));
    }
  }
}
