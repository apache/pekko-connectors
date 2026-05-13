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
import pekko.stream.{ Attributes, KillSwitches, Materializer, RestartSettings }
import pekko.stream.javadsl.{ Flow, Keep, RestartSource, Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.{
  AckDeadlineDistribution,
  AckDeadlineExtensionException,
  FlowControl
}
import pekko.stream.connectors.googlecloud.pubsub.grpc.impl.FlowControlGateStage
import pekko.{ Done, NotUsed }
import com.google.pubsub.v1._

import scala.jdk.CollectionConverters._

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

        // Don't echo initial-only fields on keepalive requests. Pub/Sub allows only
        // ackIds, modifyDeadlineSeconds, and modifyDeadlineAckIds on subsequent
        // StreamingPullRequests; anything else from the initial request (subscription,
        // streamAckDeadlineSeconds, clientId, maxOutstandingMessages, maxOutstandingBytes)
        // gets back INVALID_ARGUMENT. defaultInstance clears them all at once and stays
        // safe if the proto grows more initial-only fields.
        val subsequentRequest = StreamingPullRequest.getDefaultInstance

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
   * All errors trigger reconnection with exponential backoff, including non-retryable errors
   * such as PERMISSION_DENIED or NOT_FOUND. Use `RestartSettings.withMaxRestarts` to bound
   * the total number of restart attempts; when exhausted the stream fails with the last error.
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
   * Uses a default `maxAckExtensionPeriod` of 60 minutes (matching Google's client library).
   * Messages are NOT removed from tracking upon acknowledgment; extending the deadline of an
   * already-acknowledged message is a safe server-side no-op. For tracking that stops on
   * acknowledgment, use the adaptive variant with [[AckDeadlineDistribution]].
   *
   * If the background lease-extension ticker fails (e.g. due to a network error in the
   * `ModifyAckDeadline` RPC), the main stream will be aborted immediately with an
   * [[org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineExtensionException]],
   * even if the stream is idle.
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
    autoExtendAckDeadlines(subscription, extensionInterval, ackDeadlineSeconds, Duration.ofMinutes(60))

  /**
   * Create a flow that automatically extends ack deadlines for messages passing through it.
   *
   * A background timer periodically sends `ModifyAckDeadline` RPCs for all tracked messages,
   * preventing Pub/Sub from redelivering them while they are being processed downstream.
   * Messages are tracked from the moment they enter the flow until the stream completes, fails,
   * or `maxAckExtensionPeriod` elapses. Messages are NOT removed from tracking upon acknowledgment;
   * extending the deadline of an already-acknowledged message is a safe server-side no-op.
   * For tracking that stops on acknowledgment, use the adaptive variant with [[AckDeadlineDistribution]].
   *
   * If the background lease-extension ticker fails (e.g. due to a network error in the
   * `ModifyAckDeadline` RPC), the main stream will be aborted immediately with an
   * [[org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.AckDeadlineExtensionException]],
   * even if the stream is idle.
   *
   * @param subscription the fully qualified subscription resource string
   * @param extensionInterval how often to extend deadlines (should be less than the ack deadline)
   * @param ackDeadlineSeconds the new deadline to set on each extension
   * @param maxAckExtensionPeriod maximum duration to keep extending a message's deadline
   * @since 2.0.0
   */
  @ApiMayChange
  def autoExtendAckDeadlines(
      subscription: String,
      extensionInterval: Duration,
      ackDeadlineSeconds: Int,
      maxAckExtensionPeriod: Duration): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        val client = subscriber(mat, attr).client
        val tracked = new ConcurrentHashMap[String, java.lang.Long]()
        val maxNanos = maxAckExtensionPeriod.toNanos
        val killSwitch = KillSwitches.shared("autoExtendAckDeadlines")

        val tickerPair = Source
          .tick(extensionInterval, extensionInterval,
            ModifyAckDeadlineRequest.getDefaultInstance)
          .mapAsync(1,
            (_: ModifyAckDeadlineRequest) => {
              val now = System.nanoTime()
              // Remove expired entries
              tracked.asScala.foreach { case (ackId, entryTime) =>
                if (now - entryTime > maxNanos) tracked.remove(ackId)
              }
              val ids = tracked.asScala.keys.toSeq
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
          .toMat(Sink.ignore(), Keep.both[pekko.actor.Cancellable, CompletionStage[Done]])
          .run(mat)

        val ticker = tickerPair.first
        val tickerDone = tickerPair.second
        tickerDone.whenComplete((_, ex) => {
          if (ex != null)
            killSwitch.abort(new AckDeadlineExtensionException(
              "Lease management ticker failed; ack deadline extensions have stopped", ex))
        })

        Flow.create[ReceivedMessage]()
          .via(killSwitch.flow[ReceivedMessage])
          .map(((msg: ReceivedMessage) => {
                tracked.put(msg.getAckId, java.lang.Long.valueOf(System.nanoTime()))
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
   * Create a flow that automatically extends ack deadlines using an adaptive deadline
   * based on observed processing times, matching Google's official client library behavior.
   *
   * The same [[AckDeadlineDistribution]] must be passed to the acknowledge/nack operators
   * so that processing latencies are recorded.
   *
   * @param subscription the fully qualified subscription resource string
   * @param extensionInterval how often to extend deadlines (should be less than the ack deadline)
   * @param distribution shared distribution that tracks processing times and computes adaptive deadlines
   * @since 2.0.0
   */
  @ApiMayChange
  def autoExtendAckDeadlines(
      subscription: String,
      extensionInterval: Duration,
      distribution: AckDeadlineDistribution): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        val client = subscriber(mat, attr).client
        val killSwitch = KillSwitches.shared("autoExtendAckDeadlines")

        val tickerPair = Source
          .tick(extensionInterval, extensionInterval,
            ModifyAckDeadlineRequest.getDefaultInstance)
          .mapAsync(1,
            (_: ModifyAckDeadlineRequest) => {
              val now = System.nanoTime()
              distribution.deliveryTimes.asScala.foreach { case (ackId, _) =>
                distribution.isExpired(ackId, now)
              }
              val ids = distribution.deliveryTimes.asScala.keys.toSeq
              if (ids.nonEmpty) {
                val deadline = distribution.currentDeadlineSeconds
                client
                  .modifyAckDeadline(
                    ModifyAckDeadlineRequest.newBuilder()
                      .setSubscription(subscription)
                      .addAllAckIds(ids.asJava)
                      .setAckDeadlineSeconds(deadline)
                      .build())
                  .thenApply[Done](_ => Done.done())
              } else
                CompletableFuture.completedFuture(Done.done())
            })
          .toMat(Sink.ignore(), Keep.both[pekko.actor.Cancellable, CompletionStage[Done]])
          .run(mat)

        val ticker = tickerPair.first
        val tickerDone = tickerPair.second
        tickerDone.whenComplete((_, ex) => {
          if (ex != null)
            killSwitch.abort(new AckDeadlineExtensionException(
              "Lease management ticker failed; ack deadline extensions have stopped", ex))
        })

        Flow.create[ReceivedMessage]()
          .via(killSwitch.flow[ReceivedMessage])
          .map(((msg: ReceivedMessage) => {
                distribution.recordDelivery(msg.getAckId)
                msg
              }): pekko.japi.function.Function[ReceivedMessage, ReceivedMessage])
          .watchTermination((_, done: CompletionStage[Done]) => {
            done.whenComplete((_, _) => {
              ticker.cancel()
              distribution.deliveryTimes.clear()
            })
            NotUsed
          })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow that modifies the ack deadline for each message using a dynamic function.
   *
   * This allows per-message deadline control — for example, setting longer deadlines for
   * messages that are expected to take longer to process.
   *
   * Returning 0 from the function is equivalent to a nack (immediate redelivery).
   *
   * @param subscription the fully qualified subscription resource string
   * @param parallelism controls how many modifications can be in-flight at any given time
   * @param deadlineFn function that computes the ack deadline in seconds for each message
   * @since 2.0.0
   */
  @ApiMayChange
  def modifyAckDeadlineDynamic(
      subscription: String,
      parallelism: Int,
      deadlineFn: pekko.japi.function.Function[ReceivedMessage, Integer])
      : Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[ReceivedMessage]()
          .mapAsync(parallelism,
            (msg: ReceivedMessage) => {
              val deadline: Int = deadlineFn(msg)
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(subscription)
                    .addAckIds(msg.getAckId)
                    .setAckDeadlineSeconds(deadline)
                    .build())
                .thenApply[ReceivedMessage](_ => msg)
            })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow that nacks messages by setting the ack deadline to 0,
   * causing Pub/Sub to immediately redeliver them.
   *
   * Accepts [[AcknowledgeRequest]] for consistency with the acknowledge API.
   *
   * @since 2.0.0
   */
  @ApiMayChange
  def nackFlow(): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(1,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(req.getSubscription)
                    .addAllAckIds(req.getAckIdsList)
                    .setAckDeadlineSeconds(0)
                    .build())
                .thenApply[AcknowledgeRequest](_ => req))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that nacks messages by setting the ack deadline to 0,
   * causing Pub/Sub to immediately redeliver them.
   *
   * The materialized value completes on stream completion.
   *
   * @param parallelism controls how many nacks can be in-flight at any given time
   * @since 2.0.0
   */
  @ApiMayChange
  def nack(parallelism: Int): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(parallelism,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(req.getSubscription)
                    .addAllAckIds(req.getAckIdsList)
                    .setAckDeadlineSeconds(0)
                    .build()))
          .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
      }
      .mapMaterializedValue(flattenCs(_))

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

  /**
   * Create a flow that gates messages based on [[FlowControl]], applying backpressure
   * when the number of outstanding (unacknowledged) messages reaches the limit.
   *
   * @param flowControl shared flow control instance
   * @since 2.0.0
   */
  @ApiMayChange
  def flowControlGate(flowControl: FlowControl): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow.fromGraph(new FlowControlGateStage(flowControl))

  /**
   * Create a flow that acknowledges messages and releases [[FlowControl]] permits.
   *
   * @param flowControl shared flow control instance
   * @since 2.0.0
   */
  @ApiMayChange
  def acknowledgeFlow(flowControl: FlowControl): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsync(1,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .acknowledge(req)
                .thenApply[AcknowledgeRequest] { _ =>
                  flowControl.release(req.getAckIdsCount)
                  req
                })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that acknowledges messages and releases [[FlowControl]] permits.
   *
   * @param parallelism controls how many acknowledgements can be in-flight at any given time
   * @param flowControl shared flow control instance
   * @since 2.0.0
   */
  @ApiMayChange
  def acknowledge(parallelism: Int, flowControl: FlowControl): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(parallelism,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .acknowledge(req)
                .thenApply[Done] { _ =>
                  flowControl.release(req.getAckIdsCount)
                  Done.done()
                })
          .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
      }
      .mapMaterializedValue(flattenCs(_))

  /**
   * Create a flow that nacks messages and releases [[FlowControl]] permits.
   *
   * @param flowControl shared flow control instance
   * @since 2.0.0
   */
  @ApiMayChange
  def nackFlow(flowControl: FlowControl): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsync(1,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(req.getSubscription)
                    .addAllAckIds(req.getAckIdsList)
                    .setAckDeadlineSeconds(0)
                    .build())
                .thenApply[AcknowledgeRequest] { _ =>
                  flowControl.release(req.getAckIdsCount)
                  req
                })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that nacks messages and releases [[FlowControl]] permits.
   *
   * @param parallelism controls how many nacks can be in-flight at any given time
   * @param flowControl shared flow control instance
   * @since 2.0.0
   */
  @ApiMayChange
  def nack(parallelism: Int, flowControl: FlowControl): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(parallelism,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(req.getSubscription)
                    .addAllAckIds(req.getAckIdsList)
                    .setAckDeadlineSeconds(0)
                    .build())
                .thenApply[Done] { _ =>
                  flowControl.release(req.getAckIdsCount)
                  Done.done()
                })
          .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
      }
      .mapMaterializedValue(flattenCs(_))

  /**
   * Create a flow that acknowledges messages and records processing latencies
   * in the [[AckDeadlineDistribution]] for adaptive deadline computation.
   *
   * @param distribution shared distribution for adaptive deadline tracking
   * @since 2.0.0
   */
  @ApiMayChange
  def acknowledgeFlow(
      distribution: AckDeadlineDistribution): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsync(1,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .acknowledge(req)
                .thenApply[AcknowledgeRequest] { _ =>
                  distribution.recordCompletions(req.getAckIdsList.asScala)
                  req
                })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that acknowledges messages and records processing latencies
   * in the [[AckDeadlineDistribution]] for adaptive deadline computation.
   *
   * @param parallelism controls how many acknowledgements can be in-flight at any given time
   * @param distribution shared distribution for adaptive deadline tracking
   * @since 2.0.0
   */
  @ApiMayChange
  def acknowledge(parallelism: Int,
      distribution: AckDeadlineDistribution): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(parallelism,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .acknowledge(req)
                .thenApply[Done] { _ =>
                  distribution.recordCompletions(req.getAckIdsList.asScala)
                  Done.done()
                })
          .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])
      }
      .mapMaterializedValue(flattenCs(_))

  /**
   * Create a flow that nacks messages and records processing latencies
   * in the [[AckDeadlineDistribution]] for adaptive deadline computation.
   *
   * @param distribution shared distribution for adaptive deadline tracking
   * @since 2.0.0
   */
  @ApiMayChange
  def nackFlow(
      distribution: AckDeadlineDistribution): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsync(1,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(req.getSubscription)
                    .addAllAckIds(req.getAckIdsList)
                    .setAckDeadlineSeconds(0)
                    .build())
                .thenApply[AcknowledgeRequest] { _ =>
                  distribution.recordCompletions(req.getAckIdsList.asScala)
                  req
                })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that nacks messages and records processing latencies
   * in the [[AckDeadlineDistribution]] for adaptive deadline computation.
   *
   * @param parallelism controls how many nacks can be in-flight at any given time
   * @param distribution shared distribution for adaptive deadline tracking
   * @since 2.0.0
   */
  @ApiMayChange
  def nack(parallelism: Int,
      distribution: AckDeadlineDistribution): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow
          .create[AcknowledgeRequest]()
          .mapAsyncUnordered(parallelism,
            (req: AcknowledgeRequest) =>
              subscriber(mat, attr).client
                .modifyAckDeadline(
                  ModifyAckDeadlineRequest.newBuilder()
                    .setSubscription(req.getSubscription)
                    .addAllAckIds(req.getAckIdsList)
                    .setAckDeadlineSeconds(0)
                    .build())
                .thenApply[Done] { _ =>
                  distribution.recordCompletions(req.getAckIdsList.asScala)
                  Done.done()
                })
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
