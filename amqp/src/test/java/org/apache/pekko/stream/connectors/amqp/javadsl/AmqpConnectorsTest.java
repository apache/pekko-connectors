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

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.KillSwitches;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.UniqueKillSwitch;
import org.apache.pekko.stream.connectors.amqp.*;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.testkit.TestSubscriber;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.stream.testkit.javadsl.TestSink;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import com.rabbitmq.client.AuthenticationFailureException;
import org.junit.*;
import scala.collection.JavaConverters;
import scala.concurrent.duration.Duration;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Needs a local running AMQP server on the default port with no password. */
public class AmqpConnectorsTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @After
  public void checkForStageLeaks() {
    StreamTestKit.assertAllStagesStopped(Materializer.matFromSystem(system));
  }

  private AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();

  @Test(expected = ConnectException.class)
  public void throwIfCanNotConnect() throws Throwable {
    final String queueName = "amqp-conn-it-spec-simple-queue-" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);

    @SuppressWarnings("unchecked")
    AmqpDetailsConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5673);

    final Sink<ByteString, CompletionStage<Done>> amqpSink =
        AmqpSink.createSimple(
            AmqpWriteSettings.create(connectionProvider)
                .withRoutingKey(queueName)
                .withDeclaration(queueDeclaration));

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    final CompletionStage<Done> result =
        Source.from(input).map(ByteString::fromString).runWith(amqpSink, system);

    try {
      result.toCompletableFuture().get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test(expected = AuthenticationFailureException.class)
  public void throwWithWrongCredentials() throws Throwable {
    final String queueName = "amqp-conn-it-spec-simple-queue-" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);

    @SuppressWarnings("unchecked")
    AmqpDetailsConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("invalid", 5673)
            .withHostsAndPorts(Collections.singletonList(Pair.create("localhost", 5672)))
            .withCredentials(AmqpCredentials.create("guest", "guest1"));

    final Sink<ByteString, CompletionStage<Done>> amqpSink =
        AmqpSink.createSimple(
            AmqpWriteSettings.create(connectionProvider)
                .withRoutingKey(queueName)
                .withDeclaration(queueDeclaration));

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    final CompletionStage<Done> result =
        Source.from(input).map(ByteString::fromString).runWith(amqpSink, system);

    try {
      result.toCompletableFuture().get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
    // assertEquals(input, result.toCompletableFuture().get(3, TimeUnit.SECONDS).stream().map(m ->
    // m.bytes().utf8String()).collect(Collectors.toList()));
  }

  @Test
  public void publishAndConsumeRpcWithoutAutoAck() throws Exception {

    final String queueName = "amqp-conn-it-spec-rpc-queue-" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");

    final Flow<WriteMessage, CommittableReadResult, CompletionStage<String>> ampqRpcFlow =
        AmqpRpcFlow.committableFlow(
            AmqpWriteSettings.create(connectionProvider)
                .withRoutingKey(queueName)
                .withDeclaration(queueDeclaration),
            10,
            1);
    Pair<CompletionStage<String>, TestSubscriber.Probe<ReadResult>> result =
        Source.from(input)
            .map(ByteString::fromString)
            .map(WriteMessage::create)
            .viaMat(ampqRpcFlow, Keep.right())
            .mapAsync(1, cm -> cm.ack().thenApply(unused -> cm.message()))
            .toMat(TestSink.probe(system), Keep.both())
            .run(system);

    result.first().toCompletableFuture().get(5, TimeUnit.SECONDS);

    Sink<WriteMessage, CompletionStage<Done>> amqpSink =
        AmqpSink.createReplyTo(AmqpReplyToSinkSettings.create(connectionProvider));

    final Source<ReadResult, NotUsed> amqpSource =
        AmqpSource.atMostOnceSource(
            NamedQueueSourceSettings.create(connectionProvider, queueName)
                .withDeclaration(queueDeclaration),
            1);

    UniqueKillSwitch sourceToSink =
        amqpSource
            .viaMat(KillSwitches.single(), Keep.right())
            .map(b -> WriteMessage.create(b.bytes()).withProperties(b.properties()))
            .to(amqpSink)
            .run(system);

    List<ReadResult> probeResult =
        JavaConverters.seqAsJavaListConverter(
                result.second().toStrict(Duration.create(3, TimeUnit.SECONDS)))
            .asJava();
    assertEquals(
        probeResult.stream().map(s -> s.bytes().utf8String()).collect(Collectors.toList()), input);
    sourceToSink.shutdown();
  }

  @Test
  public void keepConnectionOpenIfDownstreamClosesAndThereArePendingAcks() throws Exception {
    final String queueName = "amqp-conn-it-spec-simple-queue-" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);

    final Sink<ByteString, CompletionStage<Done>> amqpSink =
        AmqpSink.createSimple(
            AmqpWriteSettings.create(connectionProvider)
                .withRoutingKey(queueName)
                .withDeclaration(queueDeclaration));

    final Integer bufferSize = 10;
    final Source<CommittableReadResult, NotUsed> amqpSource =
        AmqpSource.committableSource(
            NamedQueueSourceSettings.create(connectionProvider, queueName)
                .withDeclaration(queueDeclaration),
            bufferSize);

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    Source.from(input)
        .map(ByteString::fromString)
        .runWith(amqpSink, system)
        .toCompletableFuture()
        .get(3, TimeUnit.SECONDS);

    final CompletionStage<List<CommittableReadResult>> result =
        amqpSource.take(input.size()).runWith(Sink.seq(), system);

    List<CommittableReadResult> committableMessages =
        result.toCompletableFuture().get(3, TimeUnit.SECONDS);

    assertEquals(input.size(), committableMessages.size());
    committableMessages.forEach(
        cm -> {
          try {
            cm.ack(false).toCompletableFuture().get(3, TimeUnit.SECONDS);
          } catch (Exception e) {
            fail(e.getMessage());
          }
        });
  }

  @Test
  public void setRoutingKeyPerMessageAndConsumeThemInTheSameJVM() throws Exception {
    final String exchangeName = "amqp.topic." + System.currentTimeMillis();
    final ExchangeDeclaration exchangeDeclaration =
        ExchangeDeclaration.create(exchangeName, "topic");
    final String queueName = "amqp-conn-it-spec-simple-queue-" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);
    final BindingDeclaration bindingDeclaration =
        BindingDeclaration.create(queueName, exchangeName).withRoutingKey("key.*");

    final Sink<WriteMessage, CompletionStage<Done>> amqpSink =
        AmqpSink.create(
            AmqpWriteSettings.create(connectionProvider)
                .withExchange(exchangeName)
                .withDeclarations(
                    Arrays.asList(exchangeDeclaration, queueDeclaration, bindingDeclaration)));

    final Integer bufferSize = 10;
    final Source<ReadResult, NotUsed> amqpSource =
        AmqpSource.atMostOnceSource(
            NamedQueueSourceSettings.create(connectionProvider, queueName)
                .withDeclarations(
                    Arrays.asList(exchangeDeclaration, queueDeclaration, bindingDeclaration)),
            bufferSize);

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    final List<String> routingKeys =
        input.stream().map(s -> "key." + s).collect(Collectors.toList());
    Source.from(input)
        .map(s -> WriteMessage.create(ByteString.fromString(s)).withRoutingKey("key." + s))
        .runWith(amqpSink, system);

    final List<ReadResult> result =
        amqpSource
            .take(input.size())
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(3, TimeUnit.SECONDS);

    assertEquals(
        routingKeys,
        result.stream().map(m -> m.envelope().getRoutingKey()).collect(Collectors.toList()));
    assertEquals(
        input, result.stream().map(m -> m.bytes().utf8String()).collect(Collectors.toList()));
  }
}
