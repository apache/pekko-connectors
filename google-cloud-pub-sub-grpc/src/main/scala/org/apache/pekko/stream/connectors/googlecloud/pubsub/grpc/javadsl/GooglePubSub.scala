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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl

import java.time.Duration
import java.util.concurrent.{ CompletableFuture, CompletionStage, ConcurrentHashMap }
import org.apache.pekko
import pekko.actor.Cancellable
import pekko.annotation.ApiMayChange
import pekko.stream.{ Attributes, Materializer, RestartSettings }
import pekko.stream.javadsl.{ Flow, Keep, RestartSource, Sink, Source }
import pekko.{ Done, NotUsed }
import com.google.pubsub.v1._

import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._

/**
 * Google Pub/Sub Pekko Stream operator factory.
 */
object GooglePubSub {

  /**
   * Create a flow to publish messages to Google Cloud Pub/Sub. The flow emits responses that contain published
   * message ids.
   *
   * @param parallelism controls how many messages can be in-flight at any given time
   */
  def publish(parallelism: Int): Flow[PublishRequest, PublishResponse, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[PublishRequest]()
          .mapAsyncUnordered(parallelism, publisher(mat, attr).client.publish(_))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a source that emits messages for a given subscription using a StreamingPullRequest.
   *
   * The materialized value can be used to cancel the source.
   *
   * @param request      the subscription FQRS and ack deadline fields are mandatory for the request
   * @param pollInterval time between StreamingPullRequest messages are being sent
   */
  def subscribe(request: StreamingPullRequest,
      pollInterval: Duration): Source[ReceivedMessage, CompletableFuture[Cancellable]] =
    Source
      .fromMaterializer { (mat, attr) =>
        val cancellable = new CompletableFuture[Cancellable]()

        val subsequentRequest = request.toBuilder
          .setSubscription("")
          .setStreamAckDeadlineSeconds(0)
          .build()

        subscriber(mat, attr).client
          .streamingPull(
            Source
              .single(request)
              .concat(
                Source
                  .tick(Duration.ZERO, pollInterval, subsequentRequest)
                  .mapMaterializedValue(cancellable.complete(_))))
          .mapConcat(
            // TODO uptake any fix suggested for https://contributors.scala-lang.org/t/better-type-inference-for-scala-send-us-your-problematic-cases/2410/183
            ((response: StreamingPullResponse) =>
                  response.getReceivedMessagesList): pekko.japi.function.Function[StreamingPullResponse,
              java.util.List[ReceivedMessage]])
          .mapMaterializedValue(_ => cancellable)
      }
      .mapMaterializedValue(flattenCs(_))
      .mapMaterializedValue(_.toCompletableFuture)

  /**
   * Create a source that emits messages for a given subscription using a StreamingPullRequest.
   *
   * Automatically reconnects when the server closes the StreamingPull connection (due to idle
   * timeout, rebalancing, etc.), matching the behavior of Google's official client library.
   * Retryable gRPC errors (UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL, etc.) trigger reconnection
   * with exponential backoff. Non-retryable errors (PERMISSION_DENIED, NOT_FOUND, etc.) cause
   * the stream to fail immediately.
   *
   * @param request         the subscription FQRS and ack deadline fields are mandatory for the request
   * @param pollInterval    time between StreamingPullRequest messages are being sent
   * @param restartSettings settings for the exponential backoff reconnection behavior
   * @since 2.0.0
   */
  @ApiMayChange
  def subscribe(request: StreamingPullRequest,
      pollInterval: Duration,
      restartSettings: RestartSettings): Source[ReceivedMessage, NotUsed] =
    RestartSource.withBackoff(restartSettings,
      () => subscribe(request, pollInterval).mapMaterializedValue[NotUsed](_ => NotUsed))

  /**
   * Create a source that emits messages for a given subscription using a synchronous PullRequest.
   *
   * The materialized value can be used to cancel the source.
   *
   * @param request      the subscription FQRS field is mandatory for the request
   * @param pollInterval time between PullRequest messages are being sent
   */
  def subscribePolling(
      request: PullRequest,
      pollInterval: Duration): Source[ReceivedMessage, CompletableFuture[Cancellable]] =
    Source
      .fromMaterializer { (mat, attr) =>
        val cancellable = new CompletableFuture[Cancellable]()

        val client = subscriber(mat, attr).client

        Source
          .tick(Duration.ZERO, pollInterval, request)
          .mapAsync(1, client.pull(_))
          .mapConcat(
            // TODO uptake any fix suggested for https://contributors.scala-lang.org/t/better-type-inference-for-scala-send-us-your-problematic-cases/2410/183
            ((response: PullResponse) =>
                  response.getReceivedMessagesList): pekko.japi.function.Function[PullResponse,
              java.util.List[ReceivedMessage]])
          .mapMaterializedValue(cancellable.complete(_))
          .mapMaterializedValue(_ => cancellable)
      }
      .mapMaterializedValue(flattenCs(_))
      .mapMaterializedValue(_.toCompletableFuture)

  /**
   * Create a flow that accepts ack deadline modifications.
   *
   * This can be used to extend the acknowledgement deadline for messages that require
   * longer processing times, preventing Pub/Sub from redelivering them.
   *
   * @since 2.0.0
   */
  @ApiMayChange
  def modifyAckDeadlineFlow(): Flow[ModifyAckDeadlineRequest, ModifyAckDeadlineRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[ModifyAckDeadlineRequest]()
          .mapAsyncUnordered(1,
            req =>
              subscriber(mat, attr).client.modifyAckDeadline(req)
                .thenApply[ModifyAckDeadlineRequest](_ => req))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that accepts ack deadline modifications.
   *
   * This can be used to extend the acknowledgement deadline for messages that require
   * longer processing times, preventing Pub/Sub from redelivering them.
   *
   * The materialized value completes on stream completion.
   *
   * @param parallelism controls how many modifications can be in-flight at any given time
   * @since 2.0.0
   */
  @ApiMayChange
  def modifyAckDeadline(parallelism: Int): Sink[ModifyAckDeadlineRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[ModifyAckDeadlineRequest]()
          .mapAsyncUnordered(parallelism, subscriber(mat, attr).client.modifyAckDeadline(_))
          .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
      }
      .mapMaterializedValue(flattenCs(_))

  /**
   * Create a flow that automatically extends ack deadlines for messages passing through it.
   *
   * A background timer periodically sends `ModifyAckDeadline` RPCs for all tracked messages,
   * preventing Pub/Sub from redelivering them while they are being processed downstream.
   * Messages are tracked from the moment they enter the flow until the stream completes or fails.
   *
   * Extending the deadline of an already-acknowledged message is a safe no-op.
   *
   * @param subscription the fully qualified subscription resource string
   * @param extensionInterval how often to extend deadlines (should be less than the ack deadline)
   * @param ackDeadlineSeconds the new deadline to set on each extension
   * @since 2.0.0
   */
  @ApiMayChange
  def autoExtendAckDeadlines(
      subscription: String,
      extensionInterval: Duration,
      ackDeadlineSeconds: Int): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        val client = subscriber(mat, attr).client
        val tracked = ConcurrentHashMap.newKeySet[String]()

        val ticker = Source
          .tick(extensionInterval, extensionInterval,
            ModifyAckDeadlineRequest.getDefaultInstance)
          .mapAsync(1,
            (_: ModifyAckDeadlineRequest) => {
              val ids = tracked.asScala.toSeq
              if (ids.nonEmpty)
                client
                  .modifyAckDeadline(
                    ModifyAckDeadlineRequest.newBuilder()
                      .setSubscription(subscription)
                      .addAllAckIds(ids.asJava)
                      .setAckDeadlineSeconds(ackDeadlineSeconds)
                      .build())
                  .thenApply[Done](_ => Done.done())
              else
                CompletableFuture.completedFuture(Done.done())
            })
          .to(Sink.ignore())
          .run(mat)

        Flow.create[ReceivedMessage]()
          .map(((msg: ReceivedMessage) => {
                tracked.add(msg.getAckId)
                msg
              }): pekko.japi.function.Function[ReceivedMessage, ReceivedMessage])
          .watchTermination((_, done: CompletionStage[Done]) => {
            done.whenComplete((_, _) => {
              ticker.cancel()
              tracked.clear()
            })
            NotUsed
          })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow that accepts consumed message acknowledgements.
   */
  def acknowledgeFlow(): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(1,
            req =>
              subscriber(mat, attr).client.acknowledge(req).thenApply[AcknowledgeRequest](_ => req))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that accepts consumed message acknowledgements.
   *
   * The materialized value completes on stream completion.
   *
   * @param parallelism controls how many acknowledgements can be in-flight at any given time
   */
  def acknowledge(parallelism: Int): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(parallelism, subscriber(mat, attr).client.acknowledge(_))
          .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
      }
      .mapMaterializedValue(flattenCs(_))

  private def flattenCs[T](f: CompletionStage[_ <: CompletionStage[T]]): CompletionStage[T] =
    f.thenCompose((t: CompletionStage[T]) => t)

  private def publisher(mat: Materializer, attr: Attributes) =
    attr
      .get[PubSubAttributes.Publisher]
      .map(_.publisher)
      .getOrElse(GrpcPublisherExt.get(mat.system).publisher)

  private def subscriber(mat: Materializer, attr: Attributes) =
    attr
      .get[PubSubAttributes.Subscriber]
      .map(_.subscriber)
      .getOrElse(GrpcSubscriberExt.get(mat.system).subscriber)
}
