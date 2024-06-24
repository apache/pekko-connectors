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

import com.typesafe.config.Config;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.jakartams.*;
import org.apache.pekko.stream.connectors.jakartams.javadsl.JmsConsumer;
import org.apache.pekko.stream.connectors.jakartams.javadsl.JmsConsumerControl;
import org.apache.pekko.stream.connectors.jakartams.javadsl.JmsProducer;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class JmsTxConnectorsTest {

    @Rule
    public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

    private static ActorSystem system;
    private static Config consumerConfig;
    private static Config producerConfig;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
        consumerConfig = system.settings().config().getConfig(JmsConsumerSettings.configPath());
        producerConfig = system.settings().config().getConfig(JmsProducerSettings.configPath());
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    private List<JmsTextMessage> createTestMessageList() {
        List<Integer> intsIn = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<JmsTextMessage> msgsIn = new ArrayList<>();
        for (Integer n : intsIn) {
            msgsIn.add(
                    JmsTextMessage.create(n.toString())
                            .withProperty("Number", n)
                            .withProperty("IsOdd", n % 2 == 1)
                            .withProperty("IsEven", n % 2 == 0));
        }

        return msgsIn;
    }

    @Test
    public void publishAndConsume() throws Exception {
        withConnectionFactory(
                connectionFactory -> {
                    Sink<String, CompletionStage<Done>> jmsSink =
                            JmsProducer.textSink(
                                    JmsProducerSettings.create(producerConfig, connectionFactory).withQueue("test"));

                    List<String> in = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
                    Source.from(in).runWith(jmsSink, system);

                    Source<TxEnvelope, JmsConsumerControl> jmsSource =
                            JmsConsumer.txSource(
                                    JmsConsumerSettings.create(consumerConfig, connectionFactory)
                                            .withSessionCount(5)
                                            .withQueue("test"));

                    CompletionStage<List<String>> result =
                            jmsSource
                                    .take(in.size())
                                    .map(env -> new Pair<>(env, ((TextMessage) env.message()).getText()))
                                    .map(
                                            pair -> {
                                                pair.first().commit();
                                                return pair.second();
                                            })
                                    .runWith(Sink.seq(), system);

                    List<String> out = new ArrayList<>(result.toCompletableFuture().get(3, TimeUnit.SECONDS));
                    Collections.sort(out);
                    assertEquals(in, out);
                });
    }

    @Test
    public void publishAndConsumeJmsTextMessagesWithProperties() throws Exception {
        withConnectionFactory(
                connectionFactory -> {
                    Sink<JmsTextMessage, CompletionStage<Done>> jmsSink =
                            JmsProducer.sink(
                                    JmsProducerSettings.create(producerConfig, connectionFactory).withQueue("test"));

                    List<JmsTextMessage> msgsIn = createTestMessageList();
                    Source.from(msgsIn).runWith(jmsSink, system);

                    // #source
                    Source<org.apache.pekko.stream.connectors.jakartams.TxEnvelope, JmsConsumerControl> jmsSource =
                            JmsConsumer.txSource(
                                    JmsConsumerSettings.create(system, connectionFactory)
                                            .withSessionCount(5)
                                            .withAckTimeout(Duration.ofSeconds(1))
                                            .withQueue("test"));

                    CompletionStage<List<jakarta.jms.Message>> result =
                            jmsSource
                                    .take(msgsIn.size())
                                    .map(
                                            txEnvelope -> {
                                                txEnvelope.commit();
                                                return txEnvelope.message();
                                            })
                                    .runWith(Sink.seq(), system);
                    // #source

                    List<Message> outMessages =
                            new ArrayList<>(result.toCompletableFuture().get(3, TimeUnit.SECONDS));
                    outMessages.sort(
                            (a, b) -> {
                                try {
                                    return a.getIntProperty("Number") - b.getIntProperty("Number");
                                } catch (JMSException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    int msgIdx = 0;
                    for (Message outMsg : outMessages) {
                        assertEquals(
                                outMsg.getIntProperty("Number"),
                                msgsIn.get(msgIdx).properties().get("Number").get());
                        assertEquals(
                                outMsg.getBooleanProperty("IsOdd"),
                                msgsIn.get(msgIdx).properties().get("IsOdd").get());
                        assertEquals(
                                outMsg.getBooleanProperty("IsEven"),
                                (msgsIn.get(msgIdx).properties().get("IsEven").get()));
                        msgIdx++;
                    }
                });
    }

    @Test
    public void publishAndConsumeJmsTextMessagesWithHeaders() throws Exception {
        withConnectionFactory(
                connectionFactory -> {
                    Sink<JmsTextMessage, CompletionStage<Done>> jmsSink =
                            JmsProducer.sink(
                                    JmsProducerSettings.create(producerConfig, connectionFactory).withQueue("test"));

                    List<JmsTextMessage> msgsIn =
                            createTestMessageList().stream()
                                    .map(jmsTextMessage -> jmsTextMessage.withHeader(JmsType.create("type")))
                                    .map(
                                            jmsTextMessage ->
                                                    jmsTextMessage.withHeader(JmsCorrelationId.create("correlationId")))
                                    .map(jmsTextMessage -> jmsTextMessage.withHeader(JmsReplyTo.queue("test-reply")))
                                    .collect(Collectors.toList());

                    Source.from(msgsIn).runWith(jmsSink, system);

                    Source<TxEnvelope, JmsConsumerControl> jmsSource =
                            JmsConsumer.txSource(
                                    JmsConsumerSettings.create(consumerConfig, connectionFactory)
                                            .withSessionCount(5)
                                            .withQueue("test"));

                    CompletionStage<List<Message>> result =
                            jmsSource
                                    .take(msgsIn.size())
                                    .map(
                                            env -> {
                                                env.commit();
                                                return env.message();
                                            })
                                    .runWith(Sink.seq(), system);

                    List<Message> outMessages =
                            new ArrayList<>(result.toCompletableFuture().get(3, TimeUnit.SECONDS));
                    outMessages.sort(
                            (a, b) -> {
                                try {
                                    return a.getIntProperty("Number") - b.getIntProperty("Number");
                                } catch (JMSException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    int msgIdx = 0;
                    for (Message outMsg : outMessages) {
                        assertEquals(
                                outMsg.getIntProperty("Number"),
                                msgsIn.get(msgIdx).properties().get("Number").get());
                        assertEquals(
                                outMsg.getBooleanProperty("IsOdd"),
                                msgsIn.get(msgIdx).properties().get("IsOdd").get());
                        assertEquals(
                                outMsg.getBooleanProperty("IsEven"),
                                (msgsIn.get(msgIdx).properties().get("IsEven").get()));
                        assertEquals(outMsg.getJMSType(), "type");
                        assertEquals(outMsg.getJMSCorrelationID(), "correlationId");
                        assertEquals(((ActiveMQQueue) outMsg.getJMSReplyTo()).getQueueName(), "test-reply");
                        msgIdx++;
                    }
                });
    }

    @Test
    public void publishJmsTextMessagesWithPropertiesAndConsumeThemWithASelector() throws Exception {
        withConnectionFactory(
                connectionFactory -> {
                    Sink<JmsTextMessage, CompletionStage<Done>> jmsSink =
                            JmsProducer.sink(
                                    JmsProducerSettings.create(producerConfig, connectionFactory).withQueue("test"));

                    List<JmsTextMessage> msgsIn = createTestMessageList();

                    Source.from(msgsIn).runWith(jmsSink, system);

                    Source<TxEnvelope, JmsConsumerControl> jmsSource =
                            JmsConsumer.txSource(
                                    JmsConsumerSettings.create(consumerConfig, connectionFactory)
                                            .withSessionCount(5)
                                            .withQueue("test")
                                            .withSelector("IsOdd = TRUE"));

                    List<JmsTextMessage> oddMsgsIn =
                            msgsIn.stream()
                                    .filter(msg -> Integer.valueOf(msg.body()) % 2 == 1)
                                    .collect(Collectors.toList());
                    assertEquals(5, oddMsgsIn.size());

                    CompletionStage<List<Message>> result =
                            jmsSource
                                    .take(oddMsgsIn.size())
                                    .map(
                                            env -> {
                                                env.commit();
                                                return env.message();
                                            })
                                    .runWith(Sink.seq(), system);

                    List<Message> outMessages =
                            new ArrayList<>(result.toCompletableFuture().get(3, TimeUnit.SECONDS));
                    outMessages.sort(
                            (a, b) -> {
                                try {
                                    return a.getIntProperty("Number") - b.getIntProperty("Number");
                                } catch (JMSException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    int msgIdx = 0;
                    for (Message outMsg : outMessages) {
                        assertEquals(
                                outMsg.getIntProperty("Number"),
                                oddMsgsIn.get(msgIdx).properties().get("Number").get());
                        assertEquals(
                                outMsg.getBooleanProperty("IsOdd"),
                                oddMsgsIn.get(msgIdx).properties().get("IsOdd").get());
                        assertEquals(
                                outMsg.getBooleanProperty("IsEven"),
                                (oddMsgsIn.get(msgIdx).properties().get("IsEven").get()));
                        assertEquals(1, outMsg.getIntProperty("Number") % 2);
                        msgIdx++;
                    }
                });
    }

    @Test
    public void publishAndConsumeTopic() throws Exception {
        withConnectionFactory(
                connectionFactory -> {
                    List<String> in = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
                    List<String> inNumbers =
                            IntStream.range(0, 10).boxed().map(String::valueOf).collect(Collectors.toList());

                    Sink<String, CompletionStage<Done>> jmsTopicSink =
                            JmsProducer.textSink(
                                    JmsProducerSettings.create(producerConfig, connectionFactory).withTopic("topic"));

                    Sink<String, CompletionStage<Done>> jmsTopicSink2 =
                            JmsProducer.textSink(
                                    JmsProducerSettings.create(producerConfig, connectionFactory).withTopic("topic"));

                    Source<TxEnvelope, JmsConsumerControl> jmsTopicSource =
                            JmsConsumer.txSource(
                                    JmsConsumerSettings.create(consumerConfig, connectionFactory)
                                            .withSessionCount(1)
                                            .withTopic("topic"));

                    Source<TxEnvelope, JmsConsumerControl> jmsTopicSource2 =
                            JmsConsumer.txSource(
                                    JmsConsumerSettings.create(consumerConfig, connectionFactory)
                                            .withSessionCount(1)
                                            .withTopic("topic"));

                    CompletionStage<List<String>> result =
                            jmsTopicSource
                                    .take(in.size() + inNumbers.size())
                                    .map(
                                            env -> {
                                                env.commit();
                                                return ((TextMessage) env.message()).getText();
                                            })
                                    .runWith(Sink.seq(), system)
                                    .thenApply(l -> l.stream().sorted().collect(Collectors.toList()));

                    CompletionStage<List<String>> result2 =
                            jmsTopicSource2
                                    .take(in.size() + inNumbers.size())
                                    .map(
                                            env -> {
                                                env.commit();
                                                return ((TextMessage) env.message()).getText();
                                            })
                                    .runWith(Sink.seq(), system)
                                    .thenApply(l -> l.stream().sorted().collect(Collectors.toList()));

                    Thread.sleep(500);

                    Source.from(in).runWith(jmsTopicSink, system);

                    Source.from(inNumbers).runWith(jmsTopicSink2, system);

                    assertEquals(
                            Stream.concat(in.stream(), inNumbers.stream()).sorted().collect(Collectors.toList()),
                            result.toCompletableFuture().get(5, TimeUnit.SECONDS));
                    assertEquals(
                            Stream.concat(in.stream(), inNumbers.stream()).sorted().collect(Collectors.toList()),
                            result2.toCompletableFuture().get(5, TimeUnit.SECONDS));
                });
    }

    private void withServer(ConsumerChecked<EmbeddedActiveMQResource> test) throws Exception {
        EmbeddedActiveMQResource server = new EmbeddedActiveMQResource();
        try {
            server.start();
            test.accept(server);
            Thread.sleep(500);
        } finally {
            if (server.getServer().getActiveMQServer().isStarted()) {
                server.stop();
            }
        }
    }

    private void withConnectionFactory(ConsumerChecked<ConnectionFactory> test) throws Exception {
        withServer(
                server -> test.accept(new ActiveMQConnectionFactory(server.getVmURL()))
        );
    }

    @FunctionalInterface
    private interface ConsumerChecked<T> {
        void accept(T elt) throws Exception;
    }
}
