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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc

import org.apache.pekko
import pekko.Done
import pekko.actor.{ Cancellable, ClassicActorSystemProvider }
import pekko.annotation.ApiMayChange
import pekko.stream.Materializer
import pekko.stream.scaladsl.{ Keep, Sink, Source }
import pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl.{ GrpcSubscriber, GrpcSubscriberExt }
import com.google.pubsub.v1.pubsub.{ ModifyAckDeadlineRequest, SubscriberClient }

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._

/**
 * A long-lived ack-deadline extender. Owns the tracking map and the background ticker, both of
 * which live above the lifetime of any single Pub/Sub streaming pull. Pass the same instance to
 * `GooglePubSub.autoExtendAckDeadlines` (which calls `track` on every received message) so that
 * tracking state survives stream reconnects driven by `RestartSource.withBackoff`.
 *
 * This mirrors how Google's official `google-cloud-pubsub` Java client builds `MessageDispatcher`
 * once per `StreamingSubscriberConnection` and reuses it across every gRPC stream restart. In
 * that client, the dispatcher's `pendingMessages` map and background lease-extension job both
 * persist across reconnects; only the gRPC stream itself is rebuilt. This class brings the
 * same lifecycle to the pekko-connectors subscriber.
 *
 * Two flavors:
 *
 *  - Fixed deadline: every extension uses a constant `ackDeadlineSeconds`.
 *  - Adaptive deadline: every extension reads from a shared [[AckDeadlineDistribution]], which
 *    computes the deadline from observed processing latencies (matching Google's adaptive
 *    behavior). The same distribution must be passed to the acknowledge/nack operators so that
 *    completion times are recorded.
 *
 * Lifecycle:
 *
 *  - Created via `AckDeadlineExtender(...)` (Scala) or `AckDeadlineExtender.create(...)` (Java).
 *    The background ticker starts immediately at construction.
 *  - Use the same instance for every materialization of `autoExtendAckDeadlines` you intend to
 *    share tracking state across. With `RestartSource.withBackoff`, create one extender outside
 *    the restart envelope and reference it inside.
 *  - Call `close()` on shutdown. This stops the ticker and clears the tracking map. Idempotent.
 *
 * Failure semantics:
 *
 *  - If the background ticker's `ModifyAckDeadline` RPC fails, the ticker fails and `tickerDone`
 *    completes with that exception. `GooglePubSub.autoExtendAckDeadlines(extender)` wires this
 *    into a per-materialization `KillSwitch` so the inner stream aborts with an
 *    [[AckDeadlineExtensionException]]. After such a failure the extender is unusable; create a
 *    new one to recover.
 *
 * @since 2.0.0
 */
@ApiMayChange
final class AckDeadlineExtender private (
    val subscription: String,
    val extensionInterval: FiniteDuration,
    val maxAckExtensionPeriod: FiniteDuration,
    private[grpc] val tracked: ConcurrentHashMap[String, java.lang.Long],
    private val computeDeadlineSeconds: () => Int,
    private val client: SubscriberClient,
    private[grpc] val materializer: Materializer) {

  private val maxNanos = maxAckExtensionPeriod.toNanos

  private val (ticker, _tickerDone): (Cancellable, Future[Done]) = Source
    .tick(extensionInterval, extensionInterval, ())
    .mapAsync(1) { _ =>
      val now = System.nanoTime()
      // Remove expired entries
      tracked.asScala.foreach { case (ackId, entryTime) =>
        if (now - entryTime.longValue() > maxNanos) tracked.remove(ackId)
      }
      val ids = tracked.asScala.keys.toSeq
      if (ids.nonEmpty)
        client
          .modifyAckDeadline(ModifyAckDeadlineRequest(subscription, ids, computeDeadlineSeconds()))
          .map(_ => Done)(ExecutionContext.parasitic)
      else
        Future.successful(Done)
    }
    .toMat(Sink.ignore)(Keep.both)
    .run()(materializer)

  /** Future that completes when the background ticker stops, with the cause if it failed. */
  def tickerDone: Future[Done] = _tickerDone

  /** Begin tracking an ackId. Called by `GooglePubSub.autoExtendAckDeadlines`. */
  private[grpc] def track(ackId: String): Unit =
    tracked.put(ackId, java.lang.Long.valueOf(System.nanoTime()))

  /** Number of ackIds currently being tracked. Useful for diagnostics. */
  def trackedSize: Int = tracked.size()

  /**
   * Stop the background ticker and clear the tracking map. Idempotent; subsequent calls return
   * the same `Future[Done]` from the original ticker shutdown.
   */
  def close(): Future[Done] = {
    ticker.cancel()
    tracked.clear()
    _tickerDone
  }
}

@ApiMayChange
object AckDeadlineExtender {

  /**
   * Create a fixed-deadline extender that uses the configured `subscriber` from the given actor
   * system. The background ticker starts immediately.
   */
  def apply(
      subscription: String,
      extensionInterval: FiniteDuration,
      ackDeadlineSeconds: Int)(implicit system: ClassicActorSystemProvider): AckDeadlineExtender =
    apply(subscription, extensionInterval, ackDeadlineSeconds, 60.minutes)

  /** Fixed-deadline extender with a custom `maxAckExtensionPeriod`. */
  def apply(
      subscription: String,
      extensionInterval: FiniteDuration,
      ackDeadlineSeconds: Int,
      maxAckExtensionPeriod: FiniteDuration)(
      implicit system: ClassicActorSystemProvider): AckDeadlineExtender =
    apply(subscription, extensionInterval, ackDeadlineSeconds, maxAckExtensionPeriod,
      GrpcSubscriberExt()(system).subscriber)

  /** Fixed-deadline extender with an explicit subscriber (useful for tests). */
  def apply(
      subscription: String,
      extensionInterval: FiniteDuration,
      ackDeadlineSeconds: Int,
      maxAckExtensionPeriod: FiniteDuration,
      subscriber: GrpcSubscriber)(
      implicit system: ClassicActorSystemProvider): AckDeadlineExtender =
    new AckDeadlineExtender(
      subscription = subscription,
      extensionInterval = extensionInterval,
      maxAckExtensionPeriod = maxAckExtensionPeriod,
      tracked = new ConcurrentHashMap[String, java.lang.Long](),
      computeDeadlineSeconds = () => ackDeadlineSeconds,
      client = subscriber.client,
      materializer = Materializer.matFromSystem(system.classicSystem))

  /**
   * Create an adaptive extender backed by an [[AckDeadlineDistribution]]. The extender shares
   * the distribution's `deliveryTimes` map so that completion records (from the
   * acknowledge/nack operators) and tracking entries (from the eager-pull stage) reference the
   * same state. Each tick reads the distribution's currently-computed adaptive deadline.
   */
  def apply(
      subscription: String,
      extensionInterval: FiniteDuration,
      distribution: AckDeadlineDistribution)(
      implicit system: ClassicActorSystemProvider): AckDeadlineExtender =
    apply(subscription, extensionInterval, distribution, GrpcSubscriberExt()(system).subscriber)

  /** Adaptive extender with an explicit subscriber. */
  def apply(
      subscription: String,
      extensionInterval: FiniteDuration,
      distribution: AckDeadlineDistribution,
      subscriber: GrpcSubscriber)(
      implicit system: ClassicActorSystemProvider): AckDeadlineExtender =
    new AckDeadlineExtender(
      subscription = subscription,
      extensionInterval = extensionInterval,
      maxAckExtensionPeriod = distribution.maxAckExtensionPeriodNanos.nanos,
      tracked = distribution.deliveryTimes,
      computeDeadlineSeconds = () => distribution.currentDeadlineSeconds,
      client = subscriber.client,
      materializer = Materializer.matFromSystem(system.classicSystem))

  /** Java API: fixed-deadline extender with defaults. */
  def create(
      subscription: String,
      extensionInterval: java.time.Duration,
      ackDeadlineSeconds: Int,
      system: ClassicActorSystemProvider): AckDeadlineExtender =
    apply(subscription, FiniteDuration(extensionInterval.toNanos, NANOSECONDS),
      ackDeadlineSeconds)(system)

  /** Java API: fixed-deadline extender with an explicit `maxAckExtensionPeriod`. */
  def create(
      subscription: String,
      extensionInterval: java.time.Duration,
      ackDeadlineSeconds: Int,
      maxAckExtensionPeriod: java.time.Duration,
      system: ClassicActorSystemProvider): AckDeadlineExtender =
    apply(subscription, FiniteDuration(extensionInterval.toNanos, NANOSECONDS),
      ackDeadlineSeconds, FiniteDuration(maxAckExtensionPeriod.toNanos, NANOSECONDS))(system)

  /** Java API: adaptive extender. */
  def create(
      subscription: String,
      extensionInterval: java.time.Duration,
      distribution: AckDeadlineDistribution,
      system: ClassicActorSystemProvider): AckDeadlineExtender =
    apply(subscription, FiniteDuration(extensionInterval.toNanos, NANOSECONDS),
      distribution)(system)
}
