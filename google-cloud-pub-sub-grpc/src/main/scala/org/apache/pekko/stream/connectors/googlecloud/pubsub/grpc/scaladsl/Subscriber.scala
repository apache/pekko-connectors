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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.ApiMayChange
import pekko.stream.RestartSettings
import pekko.stream.scaladsl.{ Flow, Keep, Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.{ AckDeadline, AckDeadlineExtender, FlowControl }
import com.google.pubsub.v1.pubsub._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * High-level Pub/Sub subscriber that bundles streaming pull, restart logic, ack-deadline
 * extension, and optional flow control into a single resource. Designed so the user can't
 * accidentally compose the building blocks incorrectly (the trap that motivates the
 * `AckDeadlineExtender` workaround for users who compose `subscribe` + `autoExtendAckDeadlines`
 * by hand inside their own `RestartSource`).
 *
 * Lifecycle:
 *
 *  - Create one with [[Subscriber.apply]]. The deadline-extension ticker starts at construction.
 *  - Materialize [[source]] once to start receiving messages, or use [[run]] for the simple case.
 *    The source's `watchTermination` triggers [[close]] on stream completion, so most users
 *    don't need to call close themselves.
 *  - For early shutdown or when materializing multiple times, call [[close]] explicitly. It is
 *    idempotent.
 *
 * Composition rules baked in:
 *
 *  - Restart logic wraps only the inner `subscribe`, not the deadline extender, so tracking
 *    state survives reconnect backoff (matching Google's `MessageDispatcher` lifecycle).
 *  - The deadline tracker eagerly pulls from upstream so messages are tracked the moment they
 *    arrive, even when downstream is backpressured (bug 1 fix).
 *  - The flow-control gate, if configured, also pulls eagerly so its permit counter reflects
 *    actual in-flight delivery (bug 3 fix).
 *
 * Usage:
 * {{{
 * val subscriber = Subscriber(
 *   request          = StreamingPullRequest()
 *                        .withSubscription(subscriptionFqrs)
 *                        .withStreamAckDeadlineSeconds(60)
 *                        .withMaxOutstandingMessages(1000),
 *   pollInterval     = 1.second,
 *   ackDeadline      = AckDeadline.Fixed(extensionInterval = 8.seconds, deadlineSeconds = 30),
 *   restartSettings  = Some(RestartSettings(100.millis, 10.seconds, 0.2)))
 *
 * subscriber.source
 *   .mapAsync(10)(processMessage)
 *   .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
 *   .runWith(subscriber.acknowledge(parallelism = 1))
 * }}}
 *
 * @since 2.0.0
 */
@ApiMayChange
final class Subscriber private (
    val request: StreamingPullRequest,
    val pollInterval: FiniteDuration,
    val ackDeadline: AckDeadline,
    val restartSettings: Option[RestartSettings],
    val flowControl: Option[FlowControl],
    private[grpc] val extender: AckDeadlineExtender,
    private[grpc] val grpcSubscriber: GrpcSubscriber)(implicit system: ClassicActorSystemProvider) {

  private val subscription: String = request.subscription

  private def attrs = PubSubAttributes.subscriber(grpcSubscriber)

  /**
   * Source emitting messages from this subscription. Composes (in order):
   *  1. `subscribe` (wrapped in `RestartSource.withBackoff` if `restartSettings` is set)
   *  2. `autoExtendAckDeadlines(extender)` for restart-safe deadline extension
   *  3. `flowControlGate(flowControl)` if `flowControl` is set
   *
   * Restart preserves in-flight state. `RestartSource` re-materializes only the inner
   * `subscribe`; the eager-pull tracker, the flow-control gate, and any downstream operators
   * stay alive across every gRPC stream restart. Messages buffered in those operators at the
   * moment of disconnect are NOT lost. They remain in their internal buffers, keep getting
   * their deadlines extended by the long-lived ticker, and continue flowing downstream once
   * the new gRPC stream comes up. Pub/Sub ackIds are valid against the subscription rather
   * than any particular stream, so acks against pre-disconnect messages still succeed.
   *
   * On stream completion (success or failure), the extender is closed automatically.
   * Materializing this source more than once is supported, but only the first completion
   * triggers auto-cleanup; subsequent materializations after auto-cleanup will fail because
   * the extender is closed. Use [[close]] explicitly for that case.
   */
  def source: Source[ReceivedMessage, NotUsed] = {
    val baseSource: Source[ReceivedMessage, NotUsed] = restartSettings match {
      case Some(rs) => GooglePubSub.subscribe(request, pollInterval, rs).withAttributes(attrs)
      case None     =>
        GooglePubSub.subscribe(request, pollInterval).mapMaterializedValue(_ => NotUsed).withAttributes(attrs)
    }

    val withExtender = baseSource.via(GooglePubSub.autoExtendAckDeadlines(extender))

    val withFlowControl = flowControl match {
      case Some(fc) => withExtender.via(GooglePubSub.flowControlGate(fc))
      case None     => withExtender
    }

    withFlowControl.watchTermination { (_, done: Future[Done]) =>
      done.onComplete(_ => close())(ExecutionContext.parasitic)
      NotUsed
    }
  }

  /**
   * Sink that acknowledges messages. Releases flow-control permits if this subscriber was
   * configured with a `FlowControl`. Records completion latencies into the
   * [[AckDeadlineDistribution]] if this subscriber uses [[AckDeadline.Adaptive]].
   */
  def acknowledge(parallelism: Int): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, _) =>
        val client = grpcSubscriber.client
        val ec = mat.executionContext
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism) { req =>
            client.acknowledge(req).map { _ =>
              flowControl.foreach(_.release(req.ackIds.size))
              ackDeadline match {
                case AckDeadline.Adaptive(_, dist) => dist.recordCompletions(req.ackIds)
                case _                             => ()
              }
              Done
            }(ec)
          }
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

  /**
   * Sink that nacks messages by setting their ack deadline to 0, causing immediate redelivery.
   * Releases flow-control permits and records completions same as [[acknowledge]].
   */
  def nack(parallelism: Int): Sink[AcknowledgeRequest, Future[Done]] =
    Sink
      .fromMaterializer { (mat, _) =>
        val client = grpcSubscriber.client
        val ec = mat.executionContext
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism) { req =>
            client.modifyAckDeadline(
              ModifyAckDeadlineRequest(req.subscription, req.ackIds, ackDeadlineSeconds = 0))
              .map { _ =>
                flowControl.foreach(_.release(req.ackIds.size))
                ackDeadline match {
                  case AckDeadline.Adaptive(_, dist) => dist.recordCompletions(req.ackIds)
                  case _                             => ()
                }
                Done
              }(ec)
          }
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContext.parasitic))

  /**
   * Convenience for the simple case: materialize the source, apply `processFn` with the given
   * parallelism, and ack each successfully-processed message. Returns the materialized future
   * that completes when the stream completes.
   */
  def run(parallelism: Int)(processFn: ReceivedMessage => Future[Any]): Future[Done] = {
    implicit val ec: ExecutionContext = ExecutionContext.parasitic
    source
      .mapAsync(parallelism)(msg => processFn(msg).map(_ => msg))
      .map(msg => AcknowledgeRequest(subscription, Seq(msg.ackId)))
      .runWith(acknowledge(parallelism = 1))
  }

  /** Stop the background ticker and clear tracking state. Idempotent. */
  def close(): Future[Done] = extender.close()
}

@ApiMayChange
object Subscriber {

  /**
   * INTERNAL: package-private constructor. Public entry point is
   * [[GooglePubSub.subscriber]].
   */
  private[grpc] def create(
      request: StreamingPullRequest,
      pollInterval: FiniteDuration,
      ackDeadline: AckDeadline,
      restartSettings: Option[RestartSettings],
      flowControl: Option[FlowControl],
      grpcSubscriber: GrpcSubscriber)(
      implicit system: ClassicActorSystemProvider): Subscriber = {
    val extender = ackDeadline match {
      case AckDeadline.Fixed(extensionInterval, deadlineSecs, maxExt) =>
        AckDeadlineExtender(request.subscription, extensionInterval, deadlineSecs, maxExt,
          grpcSubscriber)
      case AckDeadline.Adaptive(extensionInterval, distribution) =>
        AckDeadlineExtender(request.subscription, extensionInterval, distribution, grpcSubscriber)
    }
    new Subscriber(request, pollInterval, ackDeadline, restartSettings, flowControl,
      extender, grpcSubscriber)
  }
}
