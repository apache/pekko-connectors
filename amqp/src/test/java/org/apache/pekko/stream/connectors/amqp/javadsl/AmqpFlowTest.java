/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.amqp.javadsl;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.amqp.AmqpLocalConnectionProvider;
import org.apache.pekko.stream.connectors.amqp.AmqpWriteSettings;
import org.apache.pekko.stream.connectors.amqp.QueueDeclaration;
import org.apache.pekko.stream.connectors.amqp.WriteMessage;
import org.apache.pekko.stream.connectors.amqp.WriteResult;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.FlowWithContext;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.testkit.TestSubscriber;
import org.apache.pekko.stream.testkit.javadsl.TestSink;
import org.apache.pekko.util.ByteString;
import scala.collection.JavaConverters;

/** Needs a local running AMQP server on the default port with no password. */
public class AmqpFlowTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }

  private static AmqpWriteSettings settings() {
    final String queueName = "amqp-flow-spec" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);

    return AmqpWriteSettings.create(AmqpLocalConnectionProvider.getInstance())
        .withRoutingKey(queueName)
        .withDeclaration(queueDeclaration)
        .withBufferSize(10)
        .withConfirmationTimeout(Duration.ofMillis(200));
  }

  @Test
  public void shouldEmitConfirmationForPublishedMessagesInSimpleFlow() {
    shouldEmitConfirmationForPublishedMessages(AmqpFlow.create(settings()));
  }

  @Test
  public void shouldEmitConfirmationForPublishedMessagesInFlowWithConfirm() {
    shouldEmitConfirmationForPublishedMessages(AmqpFlow.createWithConfirm(settings()));
  }

  @Test
  public void shouldEmitConfirmationForPublishedMessagesInFlowWithConfirmUnordered() {
    shouldEmitConfirmationForPublishedMessages(AmqpFlow.createWithConfirmUnordered(settings()));
  }

  private void shouldEmitConfirmationForPublishedMessages(
      final Flow<WriteMessage, WriteResult, CompletionStage<Done>> flow) {

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    final List<WriteResult> expectedOutput =
        input.stream().map(pt -> WriteResult.create(true)).collect(Collectors.toList());

    final TestSubscriber.Probe<WriteResult> result =
        Source.from(input)
            .map(s -> WriteMessage.create(ByteString.fromString(s)))
            .via(flow)
            .toMat(TestSink.probe(system), Keep.right())
            .run(system);

    result
        .request(input.size())
        .expectNextN(JavaConverters.asScalaBufferConverter(expectedOutput).asScala().toList());
  }

  @Test
  public void shouldPropagateContextInSimpleFlow() {
    shouldPropagateContext(AmqpFlowWithContext.create(settings()));
  }

  @Test
  public void shouldPropagateContextInFlowWithConfirm() {
    shouldPropagateContext(AmqpFlowWithContext.createWithConfirm(settings()));
  }

  private void shouldPropagateContext(
      FlowWithContext<WriteMessage, String, WriteResult, String, CompletionStage<Done>>
          flowWithContext) {

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    final List<Pair<WriteResult, String>> expectedOutput =
        input.stream()
            .map(pt -> Pair.create(WriteResult.create(true), pt))
            .collect(Collectors.toList());

    final TestSubscriber.Probe<Pair<WriteResult, String>> result =
        Source.from(input)
            .asSourceWithContext(s -> s)
            .map(s -> WriteMessage.create(ByteString.fromString(s)))
            .via(flowWithContext)
            .asSource()
            .toMat(TestSink.probe(system), Keep.right())
            .run(system);

    result
        .request(input.size())
        .expectNextN(JavaConverters.asScalaBufferConverter(expectedOutput).asScala().toList());
  }

  @Test
  public void shouldPropagatePassThrough() {
    Flow<Pair<WriteMessage, String>, Pair<WriteResult, String>, CompletionStage<Done>> flow =
        AmqpFlow.createWithConfirmAndPassThroughUnordered(settings());

    final List<String> input = Arrays.asList("one", "two", "three", "four", "five");
    final List<Pair<WriteResult, String>> expectedOutput =
        input.stream()
            .map(pt -> Pair.create(WriteResult.create(true), pt))
            .collect(Collectors.toList());

    final TestSubscriber.Probe<Pair<WriteResult, String>> result =
        Source.from(input)
            .map(s -> Pair.create(WriteMessage.create(ByteString.fromString(s)), s))
            .via(flow)
            .toMat(TestSink.probe(system), Keep.right())
            .run(system);

    result
        .request(input.size())
        .expectNextN(JavaConverters.asScalaBufferConverter(expectedOutput).asScala().toList());
  }
}
