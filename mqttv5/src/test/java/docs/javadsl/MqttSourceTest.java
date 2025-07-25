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
import org.apache.pekko.stream.KillSwitches;
import org.apache.pekko.stream.UniqueKillSwitch;
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings;
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage;
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS;
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions;
import org.apache.pekko.stream.connectors.mqttv5.javadsl.MqttMessageWithAck;
import org.apache.pekko.stream.connectors.mqttv5.javadsl.MqttSink;
import org.apache.pekko.stream.connectors.mqttv5.javadsl.MqttSource;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.*;
import org.apache.pekko.stream.testkit.TestSubscriber;
import org.apache.pekko.stream.testkit.javadsl.TestSink;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MqttSourceTest {

    @Rule
    public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

    private static final Logger log = LoggerFactory.getLogger(MqttSourceTest.class);

    private static ActorSystem system;

    private static final int bufferSize = 8;

    @BeforeClass
    public static void setup() throws Exception {
        system = ActorSystem.create("MqttSourceTest");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void connectionSettings() {
        // #create-connection-settings
        MqttConnectionSettings connectionSettings =
                MqttConnectionSettings.create(
                        "tcp://localhost:1883", // (1)
                        "test-java-client", // (2)
                        new MemoryPersistence() // (3)
                );
        // #create-connection-settings
        assertThat(connectionSettings.toString(), containsString("tcp://localhost:1883"));
    }

    @Test
    public void connectionSettingsForSsl() throws Exception {
        // #ssl-settings
        MqttConnectionSettings connectionSettings =
                MqttConnectionSettings.create("ssl://localhost:1885", "ssl-client", new MemoryPersistence())
                        .withAuth("mqttUser", "mqttPassword")
                        .withSocketFactory(SSLContext.getDefault().getSocketFactory());
        // #ssl-settings
        assertThat(connectionSettings.toString(), containsString("ssl://localhost:1885"));
        assertThat(connectionSettings.asMqttConnectionOptions().getUserName(), is("mqttUser"));
    }

    @Test
    public void publishAndConsumeWithoutAutoAck() throws Exception {
        final String topic = "v5/source-test/manualacks";
        final MqttConnectionSettings baseConnectionSettings =
                MqttConnectionSettings.create(
                        "tcp://localhost:1883", "test-java-client", new MemoryPersistence());

        MqttConnectionSettings connectionSettings = baseConnectionSettings;

        final List<String> input = Arrays.asList("one", "two", "three", "four", "five");

        // #create-source-with-manualacks
        Source<MqttMessageWithAck, CompletionStage<Done>> mqttSource =
                MqttSource.atLeastOnce(
                        connectionSettings
                                .withClientId("source-test/source-withoutAutoAck")
                                .withCleanStart(false),
                        MqttSubscriptions.create(topic, MqttQoS.atLeastOnce()),
                        bufferSize);
        // #create-source-with-manualacks

        final Pair<CompletionStage<Done>, CompletionStage<List<MqttMessageWithAck>>> unackedResult =
                mqttSource.take(input.size()).toMat(Sink.seq(), Keep.both()).run(system);

        unackedResult.first().toCompletableFuture().get(5, TimeUnit.SECONDS);

        final Sink<MqttMessage, CompletionStage<Done>> mqttSink =
                MqttSink.create(
                        baseConnectionSettings.withClientId("source-test/sink-withoutAutoAck"),
                        MqttQoS.atLeastOnce());
        Source.from(input)
                .map(s -> MqttMessage.create(topic, ByteString.fromString(s)))
                .runWith(mqttSink, system);

        assertEquals(
                input,
                unackedResult.second().toCompletableFuture().get(5, TimeUnit.SECONDS).stream()
                        .map(m -> m.message().payload().utf8String())
                        .collect(Collectors.toList()));

        Flow<MqttMessageWithAck, MqttMessageWithAck, NotUsed> businessLogic = Flow.create();

        // #run-source-with-manualacks
        final CompletionStage<List<MqttMessage>> result =
                mqttSource
                        .via(businessLogic)
                        .mapAsync(
                                1,
                                messageWithAck ->
                                        messageWithAck.ack().thenApply(unused2 -> messageWithAck.message()))
                        .take(input.size())
                        .runWith(Sink.seq(), system);
        // #run-source-with-manualacks

        assertEquals(
                input,
                result.toCompletableFuture().get(3, TimeUnit.SECONDS).stream()
                        .map(m -> m.payload().utf8String())
                        .collect(Collectors.toList()));
    }

    @Test
    public void keepConnectionOpenIfDownstreamClosesAndThereArePendingAcks() throws Exception {
        final String topic = "v5/source-test/pendingacks";
        final MqttConnectionSettings baseConnectionSettings =
                MqttConnectionSettings.create(
                        "tcp://localhost:1883", "test-java-client", new MemoryPersistence());

        MqttConnectionSettings sourceSettings =
                baseConnectionSettings.withClientId("source-test/source-pending");
        MqttConnectionSettings sinkSettings =
                baseConnectionSettings.withClientId("source-test/sink-pending");

        final Sink<MqttMessage, CompletionStage<Done>> mqttSink =
                MqttSink.create(sinkSettings, MqttQoS.atLeastOnce());
        final List<String> input = Arrays.asList("one", "two", "three", "four", "five");

        MqttConnectionSettings connectionSettings = sourceSettings.withCleanStart(false);
        MqttSubscriptions subscriptions = MqttSubscriptions.create(topic, MqttQoS.atLeastOnce());
        final Source<MqttMessageWithAck, CompletionStage<Done>> mqttSource =
                MqttSource.atLeastOnce(connectionSettings, subscriptions, bufferSize);

        final Pair<CompletionStage<Done>, CompletionStage<List<MqttMessageWithAck>>> unackedResult =
                mqttSource.take(input.size()).toMat(Sink.seq(), Keep.both()).run(system);

        unackedResult.first().toCompletableFuture().get(5, TimeUnit.SECONDS);

        Source.from(input)
                .map(s -> MqttMessage.create(topic, ByteString.fromString(s)))
                .runWith(mqttSink, system)
                .toCompletableFuture()
                .get(3, TimeUnit.SECONDS);

        unackedResult
                .second()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS)
                .forEach(
                        m -> {
                            try {
                                m.ack().toCompletableFuture().get(3, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                assertFalse("Error acking message manually", false);
                            }
                        });
    }

    @Test
    public void receiveFromMultipleTopics() throws Exception {
        final String topic1 = "v5/source-test/topic1";
        final String topic2 = "v5/source-test/topic2";

        MqttConnectionSettings connectionSettings =
                MqttConnectionSettings.create(
                        "tcp://localhost:1883", "test-java-client", new MemoryPersistence());

        final Integer messageCount = 7;

        // #create-source
        MqttSubscriptions subscriptions =
                MqttSubscriptions.create(topic1, MqttQoS.atMostOnce())
                        .addSubscription(topic2, MqttQoS.atMostOnce());

        Source<MqttMessage, CompletionStage<Done>> mqttSource =
                MqttSource.atMostOnce(
                        connectionSettings.withClientId("source-test/source"), subscriptions, bufferSize);

        Pair<CompletionStage<Done>, CompletionStage<List<String>>> materialized =
                mqttSource
                        .map(m -> m.topic() + "-" + m.payload().utf8String())
                        .take(messageCount * 2)
                        .toMat(Sink.seq(), Keep.both())
                        .run(system);

        CompletionStage<Done> subscribed = materialized.first();
        CompletionStage<List<String>> streamResult = materialized.second();
        // #create-source

        subscribed.toCompletableFuture().get(3, TimeUnit.SECONDS);

        List<MqttMessage> messages =
                IntStream.range(0, messageCount)
                        .boxed()
                        .flatMap(
                                i ->
                                        Stream.of(
                                                MqttMessage.create(topic1, ByteString.fromString("msg" + i.toString())),
                                                MqttMessage.create(topic2, ByteString.fromString("msg" + i.toString()))))
                        .collect(Collectors.toList());

        // #run-sink
        Sink<MqttMessage, CompletionStage<Done>> mqttSink =
                MqttSink.create(connectionSettings.withClientId("source-test/sink"), MqttQoS.atLeastOnce());
        Source.from(messages).runWith(mqttSink, system);
        // #run-sink

        assertEquals(
                IntStream.range(0, messageCount)
                        .boxed()
                        .flatMap(i -> Stream.of("v5/source-test/topic1-msg" + i, "v5/source-test/topic2-msg" + i))
                        .collect(Collectors.toSet()),
                new HashSet<>(streamResult.toCompletableFuture().get(3, TimeUnit.SECONDS)));
    }

    @Test
    public void supportWillMessage() throws Exception {
        String topic1 = "v5/source-test/topic1";
        String willTopic = "v5/source-test/will";
        final MqttConnectionSettings baseConnectionSettings =
                MqttConnectionSettings.create(
                        "tcp://localhost:1883", "test-java-client", new MemoryPersistence());
        MqttConnectionSettings sourceSettings =
                baseConnectionSettings.withClientId("source-test/source-withoutAutoAck");
        MqttConnectionSettings sinkSettings =
                baseConnectionSettings.withClientId("source-test/sink-withoutAutoAck");

        MqttMessage msg = MqttMessage.create(topic1, ByteString.fromString("ohi"));

        // #will-message
        MqttMessage lastWill =
                MqttMessage.create(willTopic, ByteString.fromString("ohi"))
                        .withQos(MqttQoS.atLeastOnce())
                        .withRetained(true);
        // #will-message

        // Create a proxy to RabbitMQ so it can be shutdown
        int proxyPort = 1347; // make sure to keep it separate from ports used by other tests
        Pair<CompletionStage<Tcp.ServerBinding>, CompletionStage<Tcp.IncomingConnection>> result1 =
                Tcp.get(system).bind("localhost", proxyPort).toMat(Sink.head(), Keep.both()).run(system);

        CompletionStage<UniqueKillSwitch> proxyKs =
                result1
                        .second()
                        .toCompletableFuture()
                        .thenApply(
                                conn ->
                                        conn.handleWith(
                                                Tcp.get(system)
                                                        .outgoingConnection("localhost", 1883)
                                                        .viaMat(KillSwitches.single(), Keep.right()),
                                                system));

        result1.first().toCompletableFuture().get(5, TimeUnit.SECONDS);

        MqttConnectionSettings settings1 =
                sourceSettings
                        .withClientId("source-test/testator")
                        .withBroker("tcp://localhost:" + proxyPort)
                        .withWill(lastWill);
        MqttSubscriptions subscriptions = MqttSubscriptions.create(topic1, MqttQoS.atLeastOnce());

        Source<MqttMessage, CompletionStage<Done>> source1 =
                MqttSource.atMostOnce(settings1, subscriptions, bufferSize);

        Pair<CompletionStage<Done>, TestSubscriber.Probe<MqttMessage>> result2 =
                source1.toMat(TestSink.probe(system), Keep.both()).run(system);

        // Ensure that the connection made it all the way to the server by waiting until it receives a
        // message
        result2.first().toCompletableFuture().get(5, TimeUnit.SECONDS);
        Source.single(msg).runWith(MqttSink.create(sinkSettings, MqttQoS.atLeastOnce()), system);
        result2.second().requestNext();

        // Kill the proxy, producing an unexpected disconnection of the client
        proxyKs.toCompletableFuture().get(5, TimeUnit.SECONDS).shutdown();

        MqttConnectionSettings settings2 = sourceSettings.withClientId("source-test/executor");
        MqttSubscriptions subscriptions2 = MqttSubscriptions.create(willTopic, MqttQoS.atLeastOnce());
        Source<MqttMessage, CompletionStage<Done>> source2 =
                MqttSource.atMostOnce(settings2, subscriptions2, bufferSize);

        CompletionStage<MqttMessage> elem = source2.runWith(Sink.head(), system);
        assertEquals(
                MqttMessage.create(willTopic, ByteString.fromString("ohi")),
                elem.toCompletableFuture().get(3, TimeUnit.SECONDS));
    }
}
