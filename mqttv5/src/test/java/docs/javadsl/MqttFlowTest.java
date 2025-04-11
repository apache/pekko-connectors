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

package docs.javadsl;

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings;
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage;
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS;
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions;
import org.apache.pekko.stream.connectors.mqttv5.javadsl.MqttFlow;
import org.apache.pekko.stream.connectors.mqttv5.javadsl.MqttMessageWithAck;
import org.apache.pekko.stream.connectors.mqttv5.javadsl.MqttMessageWithAckImpl;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertFalse;

public class MqttFlowTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static final Logger log = LoggerFactory.getLogger(MqttFlowTest.class);

  private static ActorSystem system;

  private static final int bufferSize = 8;

  @BeforeClass
  public static void setup() throws Exception {
    system = ActorSystem.create("MqttFlowTest");
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void establishBidirectionalConnectionAndSubscribeToATopic() throws Exception {
    final MqttConnectionSettings connectionSettings =
        MqttConnectionSettings.create(
            "tcp://localhost:1883", "test-java-client", new MemoryPersistence());

    // #create-flow
    final Flow<MqttMessage, MqttMessage, CompletionStage<Done>> mqttFlow =
        MqttFlow.atMostOnce(
            connectionSettings,
            MqttSubscriptions.create("v5/flow-test/topic", MqttQoS.atMostOnce()),
            bufferSize,
            MqttQoS.atLeastOnce());
    // #create-flow

    final Source<MqttMessage, CompletableFuture<Optional<MqttMessage>>> source = Source.maybe();

    // #run-flow
    final Pair<
            Pair<CompletableFuture<Optional<MqttMessage>>, CompletionStage<Done>>,
            CompletionStage<List<MqttMessage>>>
        materialized =
            source.viaMat(mqttFlow, Keep.both()).toMat(Sink.seq(), Keep.both()).run(system);

    CompletableFuture<Optional<MqttMessage>> mqttMessagePromise = materialized.first().first();
    CompletionStage<Done> subscribedToMqtt = materialized.first().second();
    CompletionStage<List<MqttMessage>> streamResult = materialized.second();
    // #run-flow

    subscribedToMqtt.thenAccept(
        a -> {
          mqttMessagePromise.complete(Optional.empty());
          assertFalse(streamResult.toCompletableFuture().isCompletedExceptionally());
        });
  }

  @Test
  public void sendAnAckAfterMessageSent() throws Exception {
    MqttMessageWithAck testMessage = new MqttMessageWithAckFake();

    final Source<MqttMessageWithAck, NotUsed> source = Source.single(testMessage);

    final MqttConnectionSettings connectionSettings =
        MqttConnectionSettings.create(
            "tcp://localhost:1883", "test-java-client-ack", new MemoryPersistence());
    // #create-flow-ack
    final Flow<MqttMessageWithAck, MqttMessageWithAck, CompletionStage<Done>> mqttFlow =
        MqttFlow.atLeastOnceWithAck(
            connectionSettings,
            MqttSubscriptions.create("v5/flow-test/topic-ack", MqttQoS.atMostOnce()),
            bufferSize,
            MqttQoS.atLeastOnce());
    // #create-flow-ack

    // #run-flow-ack
    final Pair<Pair<NotUsed, CompletionStage<Done>>, CompletionStage<List<MqttMessageWithAck>>>
        materialized =
            source.viaMat(mqttFlow, Keep.both()).toMat(Sink.seq(), Keep.both()).run(system);

    // #run-flow-ack

    for (int i = 0; (i < 10 && !((MqttMessageWithAckFake) testMessage).acked); i++) {
      Thread.sleep(1000);
    }

    assert ((MqttMessageWithAckFake) testMessage).acked;
  }

  class MqttMessageWithAckFake extends MqttMessageWithAckImpl {
    Boolean acked;

    MqttMessageWithAckFake() {
      acked = false;
    }

    @Override
    public CompletionStage<Done> ack() {
      acked = true;
      System.out.println("[MqttMessageWithAckImpl]");
      return CompletableFuture.completedFuture(Done.getInstance());
    }

    @Override
    public MqttMessage message() {
      return MqttMessage.create("topic", ByteString.fromString("hi!"));
    }
  }
}
