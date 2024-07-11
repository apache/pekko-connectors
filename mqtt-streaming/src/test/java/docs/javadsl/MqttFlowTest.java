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
import org.apache.pekko.japi.JavaPartialFunction;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.KillSwitches;
import org.apache.pekko.stream.OverflowStrategy;
import org.apache.pekko.stream.SystemMaterializer;
import org.apache.pekko.stream.UniqueKillSwitch;
import org.apache.pekko.stream.connectors.mqtt.streaming.Command;
import org.apache.pekko.stream.connectors.mqtt.streaming.ConnAck;
import org.apache.pekko.stream.connectors.mqtt.streaming.ConnAckFlags;
import org.apache.pekko.stream.connectors.mqtt.streaming.ConnAckReturnCode;
import org.apache.pekko.stream.connectors.mqtt.streaming.Connect;
import org.apache.pekko.stream.connectors.mqtt.streaming.ConnectFlags;
import org.apache.pekko.stream.connectors.mqtt.streaming.ControlPacket;
import org.apache.pekko.stream.connectors.mqtt.streaming.ControlPacketFlags;
import org.apache.pekko.stream.connectors.mqtt.streaming.DecodeErrorOrEvent;
import org.apache.pekko.stream.connectors.mqtt.streaming.Event;
import org.apache.pekko.stream.connectors.mqtt.streaming.MqttSessionSettings;
import org.apache.pekko.stream.connectors.mqtt.streaming.PubAck;
import org.apache.pekko.stream.connectors.mqtt.streaming.Publish;
import org.apache.pekko.stream.connectors.mqtt.streaming.SubAck;
import org.apache.pekko.stream.connectors.mqtt.streaming.Subscribe;
import org.apache.pekko.stream.connectors.mqtt.streaming.javadsl.ActorMqttClientSession;
import org.apache.pekko.stream.connectors.mqtt.streaming.javadsl.ActorMqttServerSession;
import org.apache.pekko.stream.connectors.mqtt.streaming.javadsl.Mqtt;
import org.apache.pekko.stream.connectors.mqtt.streaming.javadsl.MqttClientSession;
import org.apache.pekko.stream.connectors.mqtt.streaming.javadsl.MqttServerSession;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.javadsl.SourceQueueWithComplete;
import org.apache.pekko.stream.javadsl.Tcp;
import org.apache.pekko.stream.javadsl.BroadcastHub;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.*;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class MqttFlowTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static int TIMEOUT_SECONDS = 5;

  private static ActorSystem system;

  private static ActorSystem setupSystem() {
    final ActorSystem system = ActorSystem.create("MqttFlowTest");
    return system;
  }

  @BeforeClass
  public static void setup() {
    system = setupSystem();
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @After
  public void assertStageStopping() {
    StreamTestKit.assertAllStagesStopped(SystemMaterializer.get(system).materializer());
  }

  @Test
  public void establishClientBidirectionalConnectionAndSubscribeToATopic()
      throws InterruptedException, ExecutionException, TimeoutException {
    String clientId = "source-test/flow";
    String topic = "source-test/topic1";
    ByteString uniqueSessionId = ByteString.fromString("establishClientBidirectionalConnectionAndSubscribeToATopic-session");

    // #create-streaming-flow
    MqttSessionSettings settings = MqttSessionSettings.create();
    MqttClientSession session = ActorMqttClientSession.create(settings, system);

    Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
        Tcp.get(system).outgoingConnection("localhost", 1883);

    Flow<Command<Object>, DecodeErrorOrEvent<Object>, NotUsed> mqttFlow =
        Mqtt.clientSessionFlow(session, uniqueSessionId).join(connection);
    // #create-streaming-flow

    // #run-streaming-flow
    Pair<SourceQueueWithComplete<Command<Object>>, CompletionStage<Publish>> run =
        Source.<Command<Object>>queue(3, OverflowStrategy.fail())
            .via(mqttFlow)
            .collect(
                new JavaPartialFunction<DecodeErrorOrEvent<Object>, Publish>() {
                  @Override
                  public Publish apply(DecodeErrorOrEvent<Object> x, boolean isCheck) {
                    if (x.getEvent().isPresent() && x.getEvent().get().event() instanceof Publish)
                      return (Publish) x.getEvent().get().event();
                    else throw noMatch();
                  }
                })
            .toMat(Sink.head(), Keep.both())
            .run(system);

    SourceQueueWithComplete<Command<Object>> commands = run.first();
    commands.offer(new Command<>(new Connect(clientId, ConnectFlags.CleanSession())));
    commands.offer(new Command<>(new Subscribe(topic)));
    session.tell(
        new Command<>(
            new Publish(
                ControlPacketFlags.RETAIN() | ControlPacketFlags.QoSAtLeastOnceDelivery(),
                topic,
                ByteString.fromString("ohi"))));
    // #run-streaming-flow

    CompletionStage<Publish> event = run.second();
    Publish publishEvent = event.toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertEquals(publishEvent.topicName(), topic);
    assertEquals(publishEvent.payload(), ByteString.fromString("ohi"));

    // #run-streaming-flow

    // for shutting down properly
    commands.complete();
    commands.watchCompletion().thenAccept(done -> session.shutdown());
    // #run-streaming-flow

    // Wait until things have been torn down before considering the test complete
    commands.watchCompletion().toCompletableFuture().get();
  }

  @Test
  public void establishServerBidirectionalConnectionAndSubscribeToATopic()
      throws InterruptedException, ExecutionException, TimeoutException {
    String clientId = "flow-test/flow";
    String topic = "source-test/topic1";
    ByteString uniqueSessionId = ByteString.fromString("establishServerBidirectionalConnectionAndSubscribeToATopic-connection");
    String host = "localhost";
    int port = 9884;

    // #create-streaming-bind-flow
    MqttSessionSettings settings = MqttSessionSettings.create();
    MqttServerSession session = ActorMqttServerSession.create(settings, system);

    int maxConnections = 1;

    Source<DecodeErrorOrEvent<Object>, CompletionStage<Tcp.ServerBinding>> bindSource =
        Tcp.get(system)
            .bind(host, port)
            .flatMapMerge(
                maxConnections,
                connection -> {
                  Flow<Command<Object>, DecodeErrorOrEvent<Object>, NotUsed> mqttFlow =
                      Mqtt.serverSessionFlow(
                              session,
                              ByteString.fromArray(
                                  connection.remoteAddress().getAddress().getAddress()))
                          .join(connection.flow());

                  Pair<
                          SourceQueueWithComplete<Command<Object>>,
                          Source<DecodeErrorOrEvent<Object>, NotUsed>>
                      run =
                          Source.<Command<Object>>queue(2, OverflowStrategy.dropHead())
                              .via(mqttFlow)
                              .toMat(BroadcastHub.of(DecodeErrorOrEvent.classOf()), Keep.both())
                              .run(system);

                  SourceQueueWithComplete<Command<Object>> queue = run.first();
                  Source<DecodeErrorOrEvent<Object>, NotUsed> source = run.second();

                  CompletableFuture<Done> subscribed = new CompletableFuture<>();
                  source.runForeach(
                      deOrE -> {
                        if (deOrE.getEvent().isPresent()) {
                          Event<Object> event = deOrE.getEvent().get();
                          ControlPacket cp = event.event();
                          if (cp instanceof Connect) {
                            queue.offer(
                                new Command<>(
                                    new ConnAck(
                                        ConnAckFlags.None(),
                                        ConnAckReturnCode.ConnectionAccepted())));
                          } else if (cp instanceof Subscribe) {
                            Subscribe subscribe = (Subscribe) cp;
                            Collection<Tuple2<String, ControlPacketFlags>> topicFilters =
                                JavaConverters.asJavaCollectionConverter(subscribe.topicFilters())
                                    .asJavaCollection();
                            List<Integer> flags =
                                topicFilters.stream()
                                    .map(x -> x._2().underlying())
                                    .collect(Collectors.toList());
                            queue.offer(
                                new Command<>(
                                    new SubAck(subscribe.packetId(), flags),
                                    Optional.of(subscribed),
                                    Optional.empty()));
                          } else if (cp instanceof Publish) {
                            Publish publish = (Publish) cp;
                            if ((publish.flags() & ControlPacketFlags.RETAIN()) != 0) {
                              int packetId = publish.packetId().get().underlying();
                              queue.offer(new Command<>(new PubAck(packetId)));
                              subscribed.thenRun(() -> session.tell(new Command<>(publish)));
                            }
                          } // Ignore everything else
                        }
                      },
                      system);

                  return source;
                });
    // #create-streaming-bind-flow

    // #run-streaming-bind-flow
    Pair<CompletionStage<Tcp.ServerBinding>, UniqueKillSwitch> bindingAndSwitch =
        bindSource.viaMat(KillSwitches.single(), Keep.both()).to(Sink.ignore()).run(system);

    CompletionStage<Tcp.ServerBinding> bound = bindingAndSwitch.first();
    UniqueKillSwitch server = bindingAndSwitch.second();
    // #run-streaming-bind-flow

    bound.toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
        Tcp.get(system).outgoingConnection(host, port);

    MqttClientSession clientSession = new ActorMqttClientSession(settings, system);

    Flow<Command<Object>, DecodeErrorOrEvent<Object>, NotUsed> mqttFlow =
        Mqtt.clientSessionFlow(clientSession, uniqueSessionId).join(connection);

    Pair<SourceQueueWithComplete<Command<Object>>, CompletionStage<Publish>> run =
        Source.<Command<Object>>queue(3, OverflowStrategy.fail())
            .via(mqttFlow)
            .collect(
                new JavaPartialFunction<DecodeErrorOrEvent<Object>, Publish>() {
                  @Override
                  public Publish apply(DecodeErrorOrEvent<Object> x, boolean isCheck) {
                    if (x.getEvent().isPresent() && x.getEvent().get().event() instanceof Publish)
                      return (Publish) x.getEvent().get().event();
                    else throw noMatch();
                  }
                })
            .toMat(Sink.head(), Keep.both())
            .run(system);

    SourceQueueWithComplete<Command<Object>> commands = run.first();
    commands.offer(new Command<>(new Connect(clientId, ConnectFlags.None())));
    commands.offer(new Command<>(new Subscribe(topic)));
    clientSession.tell(
        new Command<>(
            new Publish(
                ControlPacketFlags.RETAIN() | ControlPacketFlags.QoSAtLeastOnceDelivery(),
                topic,
                ByteString.fromString("ohi"))));

    CompletionStage<Publish> event = run.second();
    Publish publish = event.toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertEquals(publish.topicName(), topic);
    assertEquals(publish.payload(), ByteString.fromString("ohi"));

    // #run-streaming-bind-flow

    // for shutting down properly
    server.shutdown();
    commands.watchCompletion().thenAccept(done -> session.shutdown());
    // #run-streaming-bind-flow
  }
}
