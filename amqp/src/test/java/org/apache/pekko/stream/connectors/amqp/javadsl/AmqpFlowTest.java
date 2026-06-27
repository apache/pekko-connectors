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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.amqp.AmqpLocalConnectionProvider;
import org.apache.pekko.stream.connectors.amqp.AmqpWriteSettings;
import org.apache.pekko.stream.connectors.amqp.QueueDeclaration;
import org.apache.pekko.stream.connectors.amqp.WriteMessage;
import org.apache.pekko.stream.connectors.amqp.WriteResult;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.FlowWithContext;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.testkit.TestSubscriber;
import org.apache.pekko.stream.testkit.javadsl.TestSink;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import scala.collection.JavaConverters;

/** Needs a local running AMQP server on the default port with no password. */
@ExtendWith(LogCapturingExtension.class)
public class AmqpFlowTest {

  private static ActorSystem system;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create();
  }

  private AmqpWriteSettings settings(boolean reuseByteArray) {
    final String queueName = "amqp-flow-spec" + System.currentTimeMillis();
    final QueueDeclaration queueDeclaration = QueueDeclaration.create(queueName);

    return AmqpWriteSettings.create(AmqpLocalConnectionProvider.getInstance())
        .withRoutingKey(queueName)
        .withDeclaration(queueDeclaration)
        .withReuseByteArray(reuseByteArray)
        .withBufferSize(10)
        .withConfirmationTimeout(Duration.ofMillis(200));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldEmitConfirmationForPublishedMessagesInSimpleFlow(boolean reuseByteArray) {
    shouldEmitConfirmationForPublishedMessages(AmqpFlow.create(settings(reuseByteArray)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldEmitConfirmationForPublishedMessagesInFlowWithConfirm(boolean reuseByteArray) {
    shouldEmitConfirmationForPublishedMessages(
        AmqpFlow.createWithConfirm(settings(reuseByteArray)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldEmitConfirmationForPublishedMessagesInFlowWithConfirmUnordered(
      boolean reuseByteArray) {
    shouldEmitConfirmationForPublishedMessages(
        AmqpFlow.createWithConfirmUnordered(settings(reuseByteArray)));
  }

  private void shouldEmitConfirmationForPublishedMessages(
      final Flow<WriteMessage, WriteResult, CompletionStage<Done>> flow) {

    final List<String> input = List.of("one", "two", "three", "four", "five");
    final List<WriteResult> expectedOutput =
        input.stream().map(pt -> WriteResult.create(true)).toList();

    final TestSubscriber.Probe<WriteResult> result =
        Source.from(input)
            .map(s -> WriteMessage.create(ByteString.fromString(s)))
            .via(flow)
            .toMat(TestSink.create(system), Keep.right())
            .run(system);

    result
        .request(input.size())
        .expectNextN(JavaConverters.asScalaBufferConverter(expectedOutput).asScala().toList());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldPropagateContextInSimpleFlow(boolean reuseByteArray) {
    shouldPropagateContext(AmqpFlowWithContext.create(settings(reuseByteArray)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldPropagateContextInFlowWithConfirm(boolean reuseByteArray) {
    shouldPropagateContext(AmqpFlowWithContext.createWithConfirm(settings(reuseByteArray)));
  }

  private void shouldPropagateContext(
      FlowWithContext<WriteMessage, String, WriteResult, String, CompletionStage<Done>>
          flowWithContext) {

    final List<String> input = List.of("one", "two", "three", "four", "five");
    final List<Pair<WriteResult, String>> expectedOutput =
        input.stream().map(pt -> Pair.create(WriteResult.create(true), pt)).toList();

    final TestSubscriber.Probe<Pair<WriteResult, String>> result =
        Source.from(input)
            .asSourceWithContext(s -> s)
            .map(s -> WriteMessage.create(ByteString.fromString(s)))
            .via(flowWithContext)
            .asSource()
            .toMat(TestSink.create(system), Keep.right())
            .run(system);

    result
        .request(input.size())
        .expectNextN(JavaConverters.asScalaBufferConverter(expectedOutput).asScala().toList());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldPropagatePassThrough(boolean reuseByteArray) {
    Flow<Pair<WriteMessage, String>, Pair<WriteResult, String>, CompletionStage<Done>> flow =
        AmqpFlow.createWithConfirmAndPassThroughUnordered(settings(reuseByteArray));

    final List<String> input = List.of("one", "two", "three", "four", "five");
    final List<Pair<WriteResult, String>> expectedOutput =
        input.stream().map(pt -> Pair.create(WriteResult.create(true), pt)).toList();

    final TestSubscriber.Probe<Pair<WriteResult, String>> result =
        Source.from(input)
            .map(s -> Pair.create(WriteMessage.create(ByteString.fromString(s)), s))
            .via(flow)
            .toMat(TestSink.create(system), Keep.right())
            .run(system);

    result
        .request(input.size())
        .expectNextN(JavaConverters.asScalaBufferConverter(expectedOutput).asScala().toList());
  }
}
