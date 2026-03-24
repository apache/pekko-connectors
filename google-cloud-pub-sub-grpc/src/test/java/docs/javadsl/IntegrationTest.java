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
import org.apache.pekko.actor.Cancellable;

import org.apache.pekko.actor.ActorSystem;

// #publish-single
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl.GooglePubSub;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl.GrpcPublisher;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl.PubSubAttributes;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;

// #publish-single

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  static final ActorSystem system = ActorSystem.create("IntegrationTest");

  @Test
  public void shouldPublishAMessage()
      throws InterruptedException, ExecutionException, TimeoutException {
    // #publish-single
    final String projectId = "pekko-connectors";
    final String topic = "simpleTopic";

    final PubsubMessage publishMessage =
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("Hello world!")).build();

    final PublishRequest publishRequest =
        PublishRequest.newBuilder()
            .setTopic("projects/" + projectId + "/topics/" + topic)
            .addMessages(publishMessage)
            .build();

    final Source<PublishRequest, NotUsed> source = Source.single(publishRequest);

    final Flow<PublishRequest, PublishResponse, NotUsed> publishFlow = GooglePubSub.publish(1);

    final CompletionStage<List<PublishResponse>> publishedMessageIds =
        source.via(publishFlow).runWith(Sink.seq(), system);
    // #publish-single

    assertTrue(
        "number of published messages should be more than 0",
        publishedMessageIds.toCompletableFuture().get(2, TimeUnit.SECONDS).size() > 0);
  }

  @Test
  public void shouldPublishBatch()
      throws InterruptedException, ExecutionException, TimeoutException {
    // #publish-fast
    final String projectId = "pekko-connectors";
    final String topic = "simpleTopic";

    final PubsubMessage publishMessage =
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("Hello world!")).build();

    final Source<PubsubMessage, NotUsed> messageSource = Source.single(publishMessage);
    final CompletionStage<List<PublishResponse>> published =
        messageSource
            .groupedWithin(1000, Duration.ofMinutes(1))
            .map(
                messages ->
                    PublishRequest.newBuilder()
                        .setTopic("projects/" + projectId + "/topics/" + topic)
                        .addAllMessages(messages)
                        .build())
            .via(GooglePubSub.publish(1))
            .runWith(Sink.seq(), system);
    // #publish-fast

    assertTrue(
        "number of published messages should be more than 0",
        published.toCompletableFuture().get(2, TimeUnit.SECONDS).size() > 0);
  }

  @Test
  public void shouldSubscribeStream()
      throws InterruptedException, ExecutionException, TimeoutException {
    // #subscribe-stream
    final String projectId = "pekko-connectors";
    final String subscription = "simpleSubscription";

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription("projects/" + projectId + "/subscriptions/" + subscription)
            .setStreamAckDeadlineSeconds(10)
            .build();

    final Duration pollInterval = Duration.ofSeconds(1);
    final Source<ReceivedMessage, CompletableFuture<Cancellable>> subscriptionSource =
        GooglePubSub.subscribe(request, pollInterval);
    // #subscribe-stream

    final CompletionStage<ReceivedMessage> first = subscriptionSource.runWith(Sink.head(), system);

    final String topic = "simpleTopic";
    final ByteString msg = ByteString.copyFromUtf8("Hello world!");

    final PubsubMessage publishMessage = PubsubMessage.newBuilder().setData(msg).build();

    final PublishRequest publishRequest =
        PublishRequest.newBuilder()
            .setTopic("projects/" + projectId + "/topics/" + topic)
            .addMessages(publishMessage)
            .build();

    Source.single(publishRequest).via(GooglePubSub.publish(1)).runWith(Sink.ignore(), system);

    assertEquals(
        "received and expected messages not the same",
        msg,
        first.toCompletableFuture().get(2, TimeUnit.SECONDS).getMessage().getData());
  }

  @Test
  public void shouldSubscribeSync()
      throws InterruptedException, ExecutionException, TimeoutException {
    // #subscribe-sync
    final String projectId = "pekko-connectors";
    final String subscription = "simpleSubscription";

    final PullRequest request =
        PullRequest.newBuilder()
            .setSubscription("projects/" + projectId + "/subscriptions/" + subscription)
            .setMaxMessages(10)
            .build();

    final Duration pollInterval = Duration.ofSeconds(1);
    final Source<ReceivedMessage, CompletableFuture<Cancellable>> subscriptionSource =
        GooglePubSub.subscribePolling(request, pollInterval);
    // #subscribe-sync

    final CompletionStage<ReceivedMessage> first = subscriptionSource.runWith(Sink.head(), system);

    final String topic = "simpleTopic";
    final ByteString msg = ByteString.copyFromUtf8("Hello world!");

    final PubsubMessage publishMessage = PubsubMessage.newBuilder().setData(msg).build();

    final PublishRequest publishRequest =
        PublishRequest.newBuilder()
            .setTopic("projects/" + projectId + "/topics/" + topic)
            .addMessages(publishMessage)
            .build();

    Source.single(publishRequest).via(GooglePubSub.publish(1)).runWith(Sink.ignore(), system);

    assertEquals(
        "received and expected messages not the same",
        msg,
        first.toCompletableFuture().get(2, TimeUnit.SECONDS).getMessage().getData());
  }

  @Test
  public void shouldAcknowledge() {
    final String projectId = "pekko-connectors";
    final String subscription = "simpleSubscription";

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription("projects/" + projectId + "/subscriptions/" + subscription)
            .setStreamAckDeadlineSeconds(10)
            .build();

    final Duration pollInterval = Duration.ofSeconds(1);
    final Source<ReceivedMessage, CompletableFuture<Cancellable>> subscriptionSource =
        GooglePubSub.subscribe(request, pollInterval);

    // #acknowledge
    final Sink<AcknowledgeRequest, CompletionStage<Done>> ackSink = GooglePubSub.acknowledge(1);

    subscriptionSource
        .map(
            receivedMessage -> {
              // do some computation
              return receivedMessage.getAckId();
            })
        .groupedWithin(10, Duration.ofSeconds(1))
        .map(acks -> AcknowledgeRequest.newBuilder().addAllAckIds(acks).build())
        .to(ackSink);
    // #acknowledge
  }

  @Test
  public void shouldModifyAckDeadline() {
    final String projectId = "pekko-connectors";
    final String subscription = "simpleSubscription";
    final String subscriptionFqrs =
        "projects/" + projectId + "/subscriptions/" + subscription;

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .setStreamAckDeadlineSeconds(10)
            .build();

    final Duration pollInterval = Duration.ofSeconds(1);
    final Source<ReceivedMessage, CompletableFuture<Cancellable>> subscriptionSource =
        GooglePubSub.subscribe(request, pollInterval);

    // #modify-ack-deadline
    subscriptionSource
        .mapAsync(
            1,
            receivedMessage -> {
              ModifyAckDeadlineRequest modifyRequest =
                  ModifyAckDeadlineRequest.newBuilder()
                      .setSubscription(subscriptionFqrs)
                      .addAckIds(receivedMessage.getAckId())
                      .setAckDeadlineSeconds(30)
                      .build();
              return Source.single(modifyRequest)
                  .via(GooglePubSub.modifyAckDeadlineFlow())
                  .runWith(Sink.head(), system)
                  .thenApply(ignore -> receivedMessage);
            })
        .map(
            msg ->
                AcknowledgeRequest.newBuilder()
                    .setSubscription(subscriptionFqrs)
                    .addAckIds(msg.getAckId())
                    .build())
        .to(GooglePubSub.acknowledge(1));
    // #modify-ack-deadline
  }

  @Test
  public void shouldAutoExtendAckDeadlines() {
    final String projectId = "pekko-connectors";
    final String subscription = "simpleSubscription";
    final String subscriptionFqrs =
        "projects/" + projectId + "/subscriptions/" + subscription;

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .setStreamAckDeadlineSeconds(10)
            .build();

    final Duration pollInterval = Duration.ofSeconds(1);
    final Source<ReceivedMessage, CompletableFuture<Cancellable>> subscriptionSource =
        GooglePubSub.subscribe(request, pollInterval);

    // #subscribe-auto-extend
    subscriptionSource
        .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, Duration.ofSeconds(3), 30))
        .map(
            msg ->
                AcknowledgeRequest.newBuilder()
                    .setSubscription(subscriptionFqrs)
                    .addAckIds(msg.getAckId())
                    .build())
        .to(GooglePubSub.acknowledge(1));
    // #subscribe-auto-extend
  }

  @Test
  public void shouldNackAndRedeliver()
      throws InterruptedException, ExecutionException, TimeoutException {
    final String projectId = "pekko-connectors";
    final String topic = "simpleTopic";
    final String subscription = "simpleSubscription";
    final String subscriptionFqrs = "projects/" + projectId + "/subscriptions/" + subscription;

    // publish a message
    final ByteString msg = ByteString.copyFromUtf8("nack-java-" + System.nanoTime());
    final PublishRequest publishRequest =
        PublishRequest.newBuilder()
            .setTopic("projects/" + projectId + "/topics/" + topic)
            .addMessages(PubsubMessage.newBuilder().setData(msg).build())
            .build();
    Source.single(publishRequest).via(GooglePubSub.publish(1)).runWith(Sink.head(), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .setStreamAckDeadlineSeconds(10)
            .build();

    // receive and nack — filter for our specific message to avoid test pollution
    ReceivedMessage received = GooglePubSub.subscribe(request, Duration.ofSeconds(1))
        .filter(m -> m.getMessage().getData().equals(msg))
        .runWith(Sink.head(), system)
        .toCompletableFuture().get(10, TimeUnit.SECONDS);

    Source.single(
            AcknowledgeRequest.newBuilder()
                .setSubscription(subscriptionFqrs)
                .addAckIds(received.getAckId())
                .build())
        .via(GooglePubSub.nackFlow())
        .runWith(Sink.head(), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);

    // should be redelivered
    ReceivedMessage redelivered = GooglePubSub.subscribe(request, Duration.ofSeconds(1))
        .filter(m -> m.getMessage().getData().equals(msg))
        .runWith(Sink.head(), system)
        .toCompletableFuture().get(15, TimeUnit.SECONDS);

    assertEquals("nacked message should be redelivered", msg, redelivered.getMessage().getData());
  }

  @Test
  public void shouldModifyAckDeadlineDynamic()
      throws InterruptedException, ExecutionException, TimeoutException {
    final String projectId = "pekko-connectors";
    final String topic = "simpleTopic";
    final String subscription = "simpleSubscription";
    final String subscriptionFqrs = "projects/" + projectId + "/subscriptions/" + subscription;

    // publish a message
    final ByteString msg = ByteString.copyFromUtf8("dynamic-java-" + System.nanoTime());
    final PublishRequest publishRequest =
        PublishRequest.newBuilder()
            .setTopic("projects/" + projectId + "/topics/" + topic)
            .addMessages(PubsubMessage.newBuilder().setData(msg).build())
            .build();
    Source.single(publishRequest).via(GooglePubSub.publish(1)).runWith(Sink.head(), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .setStreamAckDeadlineSeconds(10)
            .build();

    // subscribe, dynamically set deadline, then ack
    ReceivedMessage result = GooglePubSub.subscribe(request, Duration.ofSeconds(1))
        .take(1)
        .via(GooglePubSub.modifyAckDeadlineDynamic(subscriptionFqrs, 1, m -> 30))
        .runWith(Sink.head(), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);

    // ack the message
    Source.single(
            AcknowledgeRequest.newBuilder()
                .setSubscription(subscriptionFqrs)
                .addAckIds(result.getAckId())
                .build())
        .runWith(GooglePubSub.acknowledge(1), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  public void shouldFlowControl()
      throws InterruptedException, ExecutionException, TimeoutException {
    final String projectId = "pekko-connectors";
    final String topic = "simpleTopic";
    final String subscription = "simpleSubscription";
    final String subscriptionFqrs = "projects/" + projectId + "/subscriptions/" + subscription;

    // publish several messages
    final String prefix = "fc-java-" + System.nanoTime();
    PublishRequest.Builder publishBuilder = PublishRequest.newBuilder()
        .setTopic("projects/" + projectId + "/topics/" + topic);
    for (int i = 1; i <= 3; i++) {
      publishBuilder.addMessages(
          PubsubMessage.newBuilder()
              .setData(ByteString.copyFromUtf8(prefix + "-" + i))
              .build());
    }
    Source.single(publishBuilder.build()).via(GooglePubSub.publish(1))
        .runWith(Sink.head(), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .setStreamAckDeadlineSeconds(10)
            .build();

    final org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.FlowControl fc =
        org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.FlowControl.create(2);

    List<AcknowledgeRequest> result = GooglePubSub.subscribe(request, Duration.ofSeconds(1))
        .via(GooglePubSub.flowControlGate(fc))
        .take(3)
        .map(m -> AcknowledgeRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .addAckIds(m.getAckId())
            .build())
        .via(GooglePubSub.acknowledgeFlow(fc))
        .runWith(Sink.seq(), system)
        .toCompletableFuture().get(15, TimeUnit.SECONDS);

    assertEquals("should process all 3 messages", 3, result.size());
  }

  @Test
  public void shouldAdaptiveDeadline()
      throws InterruptedException, ExecutionException, TimeoutException {
    final String projectId = "pekko-connectors";
    final String topic = "simpleTopic";
    final String subscription = "simpleSubscription";
    final String subscriptionFqrs = "projects/" + projectId + "/subscriptions/" + subscription;

    final org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineDistribution dist =
        org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineDistribution.create();

    // initially should use default
    assertEquals("initial deadline", 60, dist.currentDeadlineSeconds());

    // publish messages
    final String prefix = "adaptive-java-" + System.nanoTime();
    PublishRequest.Builder publishBuilder = PublishRequest.newBuilder()
        .setTopic("projects/" + projectId + "/topics/" + topic);
    for (int i = 1; i <= 2; i++) {
      publishBuilder.addMessages(
          PubsubMessage.newBuilder()
              .setData(ByteString.copyFromUtf8(prefix + "-" + i))
              .build());
    }
    Source.single(publishBuilder.build()).via(GooglePubSub.publish(1))
        .runWith(Sink.head(), system)
        .toCompletableFuture().get(5, TimeUnit.SECONDS);

    final StreamingPullRequest request =
        StreamingPullRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .setStreamAckDeadlineSeconds(10)
            .build();

    List<AcknowledgeRequest> result = GooglePubSub.subscribe(request, Duration.ofSeconds(1))
        .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, Duration.ofSeconds(3), dist))
        .take(2)
        .map(m -> AcknowledgeRequest.newBuilder()
            .setSubscription(subscriptionFqrs)
            .addAckIds(m.getAckId())
            .build())
        .via(GooglePubSub.acknowledgeFlow(dist))
        .runWith(Sink.seq(), system)
        .toCompletableFuture().get(15, TimeUnit.SECONDS);

    assertEquals("should process all messages", 2, result.size());
    // adaptive deadline should be at least the minimum
    assertTrue("deadline should be >= 10", dist.currentDeadlineSeconds() >= 10);
  }

  @Test
  public void customPublisher() {
    // #attributes
    final PubSubSettings settings = PubSubSettings.create(system);
    final GrpcPublisher publisher = GrpcPublisher.create(settings, system);

    final Flow<PublishRequest, PublishResponse, NotUsed> publishFlow =
        GooglePubSub.publish(1).withAttributes(PubSubAttributes.publisher(publisher));
    // #attributes
  }

  @AfterClass
  public static void tearDown() {
    system.terminate();
  }
}
