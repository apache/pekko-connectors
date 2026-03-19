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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl

import org.apache.pekko
import pekko.actor.Cancellable
import pekko.annotation.ApiMayChange
import pekko.stream.{ Attributes, Materializer, RestartSettings }
import pekko.stream.scaladsl.{ Flow, Keep, RestartSource, Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.{ AckDeadlineDistribution, FlowControl }
import pekko.stream.connectors.googlecloud.pubsub.grpc.impl.FlowControlGateStage
import pekko.{ Done, NotUsed }
import com.google.pubsub.v1.pubsub._

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future, Promise }
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
        Flow[PublishRequest]
          .mapAsyncUnordered(parallelism)(publisher(mat, attr).client.publish)
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a source that emits messages for a given subscription using a StreamingPullRequest.
   *
   * The materialized value can be used to cancel the source.
   *
   * @param request the subscription FQRS and ack deadline fields are mandatory for the request
   * @param pollInterval time between StreamingPullRequest messages are being sent
   */
  def subscribe(
      request: StreamingPullRequest,
      pollInterval: FiniteDuration): Source[ReceivedMessage, Future[Cancellable]] =
    Source
      .fromMaterializer { (mat, attr) =>
        val cancellable = Promise[Cancellable]()

        val subsequentRequest = request
          .withSubscription("")
          .withStreamAckDeadlineSeconds(0)

        subscriber(mat, attr).client
          .streamingPull(
            Source
              .single(request)
              .concat(
                Source
                  .tick(0.seconds, pollInterval, ())
                  .map(_ => subsequentRequest)
                  .mapMaterializedValue(cancellable.success)))
          .mapConcat(_.receivedMessages.toVector)
          .mapMaterializedValue(_ => cancellable.future)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

  /**
   * Create a source that emits messages for a given subscription using a StreamingPullRequest.
   *
   * Automatically reconnects when the server closes the StreamingPull connection (due to idle
   * timeout, rebalancing, etc.), matching the behavior of Google's official client library.
   * Retryable gRPC errors (UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL, etc.) trigger reconnection
   * with exponential backoff. Non-retryable errors (PERMISSION_DENIED, NOT_FOUND, etc.) cause
   * the stream to fail immediately.
   *
   * @param request the subscription FQRS and ack deadline fields are mandatory for the request
   * @param pollInterval time between StreamingPullRequest messages are being sent
   * @param restartSettings settings for the exponential backoff reconnection behavior,
   *                        defaults match Google's client library (100ms to 10s)
   * @since 2.0.0
   */
  @ApiMayChange
  def subscribe(
      request: StreamingPullRequest,
      pollInterval: FiniteDuration,
      restartSettings: RestartSettings): Source[ReceivedMessage, NotUsed] =
    RestartSource.withBackoff(restartSettings) { () =>
      subscribe(request, pollInterval).mapMaterializedValue(_ => NotUsed)
    }

  /**
   * Create a source that emits messages for a given subscription using a synchronous PullRequest.
   *
   * The materialized value can be used to cancel the source.
   *
   * @param request the subscription FQRS field is mandatory for the request
   * @param pollInterval time between PullRequest messages are being sent
   */
  def subscribePolling(
      request: PullRequest,
      pollInterval: FiniteDuration): Source[ReceivedMessage, Future[Cancellable]] =
    Source
      .fromMaterializer { (mat, attr) =>
        val cancellable = Promise[Cancellable]()
        val client = subscriber(mat, attr).client
        Source
          .tick(0.seconds, pollInterval, request)
          .mapMaterializedValue(cancellable.success)
          .mapAsync(1)(client.pull(_))
          .mapConcat(_.receivedMessages.toVector)
          .mapMaterializedValue(_ => cancellable.future)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

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
        Flow[ModifyAckDeadlineRequest]
          .mapAsync(1)(req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(req)
              .map(_ => req)(mat.executionContext))
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
  def modifyAckDeadline(parallelism: Int): Sink[ModifyAckDeadlineRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[ModifyAckDeadlineRequest]
          .mapAsyncUnordered(parallelism)(subscriber(mat, attr).client.modifyAckDeadline)
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

  /**
   * Create a flow that automatically extends ack deadlines for messages passing through it.
   *
   * A background timer periodically sends `ModifyAckDeadline` RPCs for all tracked messages,
   * preventing Pub/Sub from redelivering them while they are being processed downstream.
   * Messages are tracked from the moment they enter the flow until the stream completes or fails.
   *
   * Extending the deadline of an already-acknowledged message is a safe no-op.
   *
   * Uses a default `maxAckExtensionPeriod` of 60 minutes (matching Google's client library).
   * After this period, deadline extensions stop for a given message and Pub/Sub will redeliver it.
   *
   * Usage:
   * {{{
   * GooglePubSub.subscribe(request, 1.second)
   *   .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 8.seconds, 30))
   *   .mapAsync(4)(processMessage)
   *   .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
   *   .to(GooglePubSub.acknowledge(parallelism = 1))
   * }}}
   *
   * @param subscription the fully qualified subscription resource string
   * @param extensionInterval how often to extend deadlines (should be less than the ack deadline)
   * @param ackDeadlineSeconds the new deadline to set on each extension
   * @since 2.0.0
   */
  @ApiMayChange
  def autoExtendAckDeadlines(
      subscription: String,
      extensionInterval: FiniteDuration,
      ackDeadlineSeconds: Int): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    autoExtendAckDeadlines(subscription, extensionInterval, ackDeadlineSeconds, 60.minutes)

  /**
   * Create a flow that automatically extends ack deadlines for messages passing through it.
   *
   * A background timer periodically sends `ModifyAckDeadline` RPCs for all tracked messages,
   * preventing Pub/Sub from redelivering them while they are being processed downstream.
   * Messages are tracked from the moment they enter the flow until the stream completes or fails.
   * After `maxAckExtensionPeriod` elapses for a given message, it is removed from tracking
   * and Pub/Sub will redeliver it if not yet acknowledged.
   *
   * Extending the deadline of an already-acknowledged message is a safe no-op.
   *
   * @param subscription the fully qualified subscription resource string
   * @param extensionInterval how often to extend deadlines (should be less than the ack deadline)
   * @param ackDeadlineSeconds the new deadline to set on each extension
   * @param maxAckExtensionPeriod maximum duration to keep extending a message's deadline (default 60 minutes)
   * @since 2.0.0
   */
  @ApiMayChange
  def autoExtendAckDeadlines(
      subscription: String,
      extensionInterval: FiniteDuration,
      ackDeadlineSeconds: Int,
      maxAckExtensionPeriod: FiniteDuration): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow.fromMaterializer { (mat, attr) =>
      val client = subscriber(mat, attr).client
      val tracked = new ConcurrentHashMap[String, Long]()
      val maxNanos = maxAckExtensionPeriod.toNanos

      @volatile var ticker: Cancellable = Cancellable.alreadyCancelled

      ticker = Source
        .tick(extensionInterval, extensionInterval, ())
        .mapAsync(1) { _ =>
          val now = System.nanoTime()
          // Remove expired entries
          tracked.asScala.foreach { case (ackId, entryTime) =>
            if (now - entryTime > maxNanos) tracked.remove(ackId)
          }
          val ids = tracked.asScala.keys.toSeq
          if (ids.nonEmpty)
            client
              .modifyAckDeadline(ModifyAckDeadlineRequest(subscription, ids, ackDeadlineSeconds))
              .map(_ => ())(ExecutionContext.parasitic)
          else
            Future.successful(())
        }
        .to(Sink.ignore)
        .run()(mat)

      val cleanup = () => {
        ticker.cancel()
        tracked.clear()
      }

      Flow[ReceivedMessage]
        .map { msg =>
          tracked.put(msg.ackId, System.nanoTime())
          msg
        }
        .watchTermination((_, done: Future[Done]) => {
          done.onComplete(_ => cleanup())(ExecutionContext.parasitic)
          NotUsed
        })
    }.mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow that automatically extends ack deadlines using an adaptive deadline
   * based on observed processing times, matching Google's official client library behavior.
   *
   * Instead of a fixed deadline, the extension deadline is computed from the 99th percentile
   * (configurable) of actual ack latencies. This means:
   *  - If messages typically process in 2s, deadlines stay short (~10s) for fast redelivery on failure
   *  - If messages take 45s, deadlines adapt upward to avoid unnecessary redelivery
   *
   * The same [[AckDeadlineDistribution]] must be passed to the acknowledge/nack operators
   * so that processing latencies are recorded.
   *
   * Usage:
   * {{{
   * val dist = AckDeadlineDistribution(initialDeadlineSeconds = 10)
   *
   * GooglePubSub.subscribe(request, 1.second, restartSettings)
   *   .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, dist))
   *   .mapAsync(4)(processMessage)
   *   .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
   *   .to(GooglePubSub.acknowledge(parallelism = 1, dist))
   * }}}
   *
   * @param subscription the fully qualified subscription resource string
   * @param extensionInterval how often to extend deadlines (should be less than the ack deadline)
   * @param distribution shared distribution that tracks processing times and computes adaptive deadlines
   * @since 2.0.0
   */
  @ApiMayChange
  def autoExtendAckDeadlines(
      subscription: String,
      extensionInterval: FiniteDuration,
      distribution: AckDeadlineDistribution): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow.fromMaterializer { (mat, attr) =>
      val client = subscriber(mat, attr).client

      @volatile var ticker: Cancellable = Cancellable.alreadyCancelled

      ticker = Source
        .tick(extensionInterval, extensionInterval, ())
        .mapAsync(1) { _ =>
          val now = System.nanoTime()
          // Remove expired entries
          distribution.deliveryTimes.asScala.foreach { case (ackId, _) =>
            distribution.isExpired(ackId, now)
          }
          val ids = distribution.deliveryTimes.asScala.keys.toSeq
          if (ids.nonEmpty) {
            val deadline = distribution.currentDeadlineSeconds
            client
              .modifyAckDeadline(ModifyAckDeadlineRequest(subscription, ids, deadline))
              .map(_ => ())(ExecutionContext.parasitic)
          } else
            Future.successful(())
        }
        .to(Sink.ignore)
        .run()(mat)

      val cleanup = () => {
        ticker.cancel()
        distribution.deliveryTimes.clear()
      }

      Flow[ReceivedMessage]
        .map { msg =>
          distribution.recordDelivery(msg.ackId)
          msg
        }
        .watchTermination((_, done: Future[Done]) => {
          done.onComplete(_ => cleanup())(ExecutionContext.parasitic)
          NotUsed
        })
    }.mapMaterializedValue(_ => NotUsed)

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
      parallelism: Int = 1)(deadlineFn: ReceivedMessage => Int): Flow[ReceivedMessage, ReceivedMessage, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow[ReceivedMessage]
          .mapAsync(parallelism) { msg =>
            val deadline = deadlineFn(msg)
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(subscription, Seq(msg.ackId), deadline))
              .map(_ => msg)(mat.executionContext)
          }
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow that nacks messages by setting the ack deadline to 0,
   * causing Pub/Sub to immediately redeliver them.
   *
   * Accepts [[AcknowledgeRequest]] for consistency with the acknowledge API —
   * the subscription and ack IDs are reused to build the underlying
   * `ModifyAckDeadlineRequest` with `ackDeadlineSeconds = 0`.
   *
   * @since 2.0.0
   */
  @ApiMayChange
  def nackFlow(): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsync(1)(req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0))
              .map(_ => req)(mat.executionContext))
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
  def nack(parallelism: Int): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism)(req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0)))
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

  /**
   * Create a flow that accepts consumed message acknowledgements.
   */
  def acknowledgeFlow(): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsync(1)(req =>
            subscriber(mat, attr).client
              .acknowledge(req)
              .map(_ => req)(mat.executionContext))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that accepts consumed message acknowledgements.
   *
   * The materialized value completes on stream completion.
   *
   * @param parallelism controls how many acknowledgements can be in-flight at any given time
   */
  def acknowledge(parallelism: Int): Sink[AcknowledgeRequest, Future[Done]] = {
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism)(subscriber(mat, attr).client.acknowledge)
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))
  }

  /**
   * Create a flow that gates messages based on [[FlowControl]], applying backpressure
   * when the number of outstanding (unacknowledged) messages reaches the limit.
   *
   * Pair with a [[FlowControl]]-aware acknowledge or nack operator to release permits.
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
        Flow[AcknowledgeRequest]
          .mapAsync(1) { req =>
            subscriber(mat, attr).client
              .acknowledge(req)
              .map { _ =>
                flowControl.release(req.ackIds.size)
                req
              }(mat.executionContext)
          }
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
  def acknowledge(parallelism: Int, flowControl: FlowControl): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism) { req =>
            subscriber(mat, attr).client
              .acknowledge(req)
              .map { _ =>
                flowControl.release(req.ackIds.size)
                Done
              }(mat.executionContext)
          }
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

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
        Flow[AcknowledgeRequest]
          .mapAsync(1) { req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0))
              .map { _ =>
                flowControl.release(req.ackIds.size)
                req
              }(mat.executionContext)
          }
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
  def nack(parallelism: Int, flowControl: FlowControl): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism) { req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0))
              .map { _ =>
                flowControl.release(req.ackIds.size)
                Done
              }(mat.executionContext)
          }
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

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
        Flow[AcknowledgeRequest]
          .mapAsync(1) { req =>
            subscriber(mat, attr).client
              .acknowledge(req)
              .map { _ =>
                distribution.recordCompletions(req.ackIds)
                req
              }(mat.executionContext)
          }
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
      distribution: AckDeadlineDistribution): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism) { req =>
            subscriber(mat, attr).client
              .acknowledge(req)
              .map { _ =>
                distribution.recordCompletions(req.ackIds)
                Done
              }(mat.executionContext)
          }
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

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
        Flow[AcknowledgeRequest]
          .mapAsync(1) { req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0))
              .map { _ =>
                distribution.recordCompletions(req.ackIds)
                req
              }(mat.executionContext)
          }
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
      distribution: AckDeadlineDistribution): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism) { req =>
            subscriber(mat, attr).client
              .modifyAckDeadline(
                ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0))
              .map { _ =>
                distribution.recordCompletions(req.ackIds)
                Done
              }(mat.executionContext)
          }
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

  private def publisher(mat: Materializer, attr: Attributes) =
    attr
      .get[PubSubAttributes.Publisher]
      .map(_.publisher)
      .getOrElse(GrpcPublisherExt()(mat.system).publisher)

  private def subscriber(mat: Materializer, attr: Attributes) =
    attr
      .get[PubSubAttributes.Subscriber]
      .map(_.subscriber)
      .getOrElse(GrpcSubscriberExt()(mat.system).subscriber)
}
