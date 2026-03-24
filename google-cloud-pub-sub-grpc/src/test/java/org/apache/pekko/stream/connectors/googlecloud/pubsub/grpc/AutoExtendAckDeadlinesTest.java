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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc;

import static org.junit.Assert.*;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl.GooglePubSub;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl.GrpcSubscriber;
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl.PubSubAttributes;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.AfterClass;
import org.junit.Test;

public class AutoExtendAckDeadlinesTest {

  private static final ActorSystem system = ActorSystem.create("AutoExtendAckDeadlinesTest");
  private static final String subscription = "projects/test/subscriptions/test-sub";

  private static ReceivedMessage makeMsg(String id) {
    return ReceivedMessage.newBuilder()
        .setAckId(id)
        .setMessage(
            PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("test-" + id)).build())
        .build();
  }

  /** Stub that fails modifyAckDeadline calls. */
  static class FailingSubscriberClient extends SubscriberClient {
    @Override
    public CompletionStage<com.google.protobuf.Empty> modifyAckDeadline(
        ModifyAckDeadlineRequest in) {
      CompletableFuture<com.google.protobuf.Empty> f = new CompletableFuture<>();
      f.completeExceptionally(new RuntimeException("simulated modifyAckDeadline failure"));
      return f;
    }

    @Override
    public CompletionStage<com.google.protobuf.Empty> acknowledge(AcknowledgeRequest in) {
      return CompletableFuture.completedFuture(com.google.protobuf.Empty.getDefaultInstance());
    }

    @Override
    public CompletionStage<Subscription> createSubscription(Subscription in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Subscription> getSubscription(GetSubscriptionRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Subscription> updateSubscription(UpdateSubscriptionRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<ListSubscriptionsResponse> listSubscriptions(
        ListSubscriptionsRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<com.google.protobuf.Empty> deleteSubscription(
        DeleteSubscriptionRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<PullResponse> pull(PullRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source<StreamingPullResponse, NotUsed> streamingPull(
        Source<StreamingPullRequest, NotUsed> in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<com.google.protobuf.Empty> modifyPushConfig(ModifyPushConfigRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Snapshot> getSnapshot(GetSnapshotRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<ListSnapshotsResponse> listSnapshots(ListSnapshotsRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Snapshot> createSnapshot(CreateSnapshotRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Snapshot> updateSnapshot(UpdateSnapshotRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<com.google.protobuf.Empty> deleteSnapshot(DeleteSnapshotRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<SeekResponse> seek(SeekRequest in) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Done> close() {
      return CompletableFuture.completedFuture(Done.done());
    }

    @Override
    public CompletionStage<Done> closed() {
      return new CompletableFuture<>(); // never completes
    }
  }

  /** Stub that succeeds for modifyAckDeadline calls. */
  static class SucceedingSubscriberClient extends FailingSubscriberClient {
    @Override
    public CompletionStage<com.google.protobuf.Empty> modifyAckDeadline(
        ModifyAckDeadlineRequest in) {
      return CompletableFuture.completedFuture(com.google.protobuf.Empty.getDefaultInstance());
    }
  }

  @Test
  public void fixedDeadlineShouldFailStreamWhenTickerFails() throws Exception {
    GrpcSubscriber testSubscriber = new GrpcSubscriber(new FailingSubscriberClient());

    List<ReceivedMessage> msgs = Arrays.asList(makeMsg("1"), makeMsg("2"), makeMsg("3"));

    CompletionStage<List<ReceivedMessage>> result =
        Source.from(msgs)
            .throttle(1, Duration.ofMillis(500))
            .via(GooglePubSub.autoExtendAckDeadlines(subscription, Duration.ofMillis(200), 30))
            .withAttributes(PubSubAttributes.subscriber(testSubscriber))
            .runWith(Sink.seq(), system);

    try {
      result.toCompletableFuture().get(10, TimeUnit.SECONDS);
      fail("Expected AckDeadlineExtensionException");
    } catch (ExecutionException e) {
      assertTrue(
          "Expected AckDeadlineExtensionException but got: " + e.getCause().getClass(),
          e.getCause() instanceof AckDeadlineExtensionException);
      assertTrue(e.getCause().getMessage().contains("ticker failed"));
    }
  }

  @Test
  public void fixedDeadlineShouldFailIdleStreamWhenTickerDies() throws Exception {
    GrpcSubscriber testSubscriber = new GrpcSubscriber(new FailingSubscriberClient());

    // Emit one message then idle — the ticker should abort the stream
    // even though no further elements arrive.
    CompletionStage<List<ReceivedMessage>> result =
        Source.single(makeMsg("only"))
            .concat(Source.maybe())
            .via(GooglePubSub.autoExtendAckDeadlines(subscription, Duration.ofMillis(200), 30))
            .withAttributes(PubSubAttributes.subscriber(testSubscriber))
            .runWith(Sink.seq(), system);

    try {
      result.toCompletableFuture().get(10, TimeUnit.SECONDS);
      fail("Expected AckDeadlineExtensionException");
    } catch (ExecutionException e) {
      assertTrue(
          "Expected AckDeadlineExtensionException but got: " + e.getCause().getClass(),
          e.getCause() instanceof AckDeadlineExtensionException);
      assertTrue(e.getCause().getMessage().contains("ticker failed"));
    }
  }

  @Test
  public void fixedDeadlineShouldPassMessagesThroughWhenHealthy() throws Exception {
    GrpcSubscriber testSubscriber = new GrpcSubscriber(new SucceedingSubscriberClient());

    List<ReceivedMessage> msgs = Arrays.asList(makeMsg("1"), makeMsg("2"), makeMsg("3"));

    List<ReceivedMessage> result =
        Source.from(msgs)
            .via(GooglePubSub.autoExtendAckDeadlines(subscription, Duration.ofSeconds(1), 30))
            .withAttributes(PubSubAttributes.subscriber(testSubscriber))
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

    assertEquals(3, result.size());
    assertEquals("1", result.get(0).getAckId());
    assertEquals("2", result.get(1).getAckId());
    assertEquals("3", result.get(2).getAckId());
  }

  @Test
  public void adaptiveShouldFailStreamWhenTickerFails() throws Exception {
    GrpcSubscriber testSubscriber = new GrpcSubscriber(new FailingSubscriberClient());
    AckDeadlineDistribution dist = AckDeadlineDistribution.create();

    List<ReceivedMessage> msgs = Arrays.asList(makeMsg("1"), makeMsg("2"), makeMsg("3"));

    CompletionStage<List<ReceivedMessage>> result =
        Source.from(msgs)
            .throttle(1, Duration.ofMillis(500))
            .via(GooglePubSub.autoExtendAckDeadlines(subscription, Duration.ofMillis(200), dist))
            .withAttributes(PubSubAttributes.subscriber(testSubscriber))
            .runWith(Sink.seq(), system);

    try {
      result.toCompletableFuture().get(10, TimeUnit.SECONDS);
      fail("Expected AckDeadlineExtensionException");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof AckDeadlineExtensionException);
      assertTrue(e.getCause().getMessage().contains("ticker failed"));
    }
  }

  @Test
  public void adaptiveShouldFailIdleStreamWhenTickerDies() throws Exception {
    GrpcSubscriber testSubscriber = new GrpcSubscriber(new FailingSubscriberClient());
    AckDeadlineDistribution dist = AckDeadlineDistribution.create();

    CompletionStage<List<ReceivedMessage>> result =
        Source.single(makeMsg("only"))
            .concat(Source.maybe())
            .via(GooglePubSub.autoExtendAckDeadlines(subscription, Duration.ofMillis(200), dist))
            .withAttributes(PubSubAttributes.subscriber(testSubscriber))
            .runWith(Sink.seq(), system);

    try {
      result.toCompletableFuture().get(10, TimeUnit.SECONDS);
      fail("Expected AckDeadlineExtensionException");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof AckDeadlineExtensionException);
      assertTrue(e.getCause().getMessage().contains("ticker failed"));
    }
  }

  @Test
  public void adaptiveShouldPassMessagesThroughWhenHealthy() throws Exception {
    GrpcSubscriber testSubscriber = new GrpcSubscriber(new SucceedingSubscriberClient());
    AckDeadlineDistribution dist = AckDeadlineDistribution.create();

    List<ReceivedMessage> msgs = Arrays.asList(makeMsg("1"), makeMsg("2"), makeMsg("3"));

    List<ReceivedMessage> result =
        Source.from(msgs)
            .via(GooglePubSub.autoExtendAckDeadlines(subscription, Duration.ofSeconds(1), dist))
            .withAttributes(PubSubAttributes.subscriber(testSubscriber))
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

    assertEquals(3, result.size());
  }

  @AfterClass
  public static void tearDown() {
    system.terminate();
  }
}
