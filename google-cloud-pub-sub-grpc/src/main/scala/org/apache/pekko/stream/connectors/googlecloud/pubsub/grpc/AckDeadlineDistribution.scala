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

import org.apache.pekko.annotation.ApiMayChange

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLongArray }

/**
 * Tracks message processing latencies and computes an adaptive ack deadline
 * based on the percentile of observed processing times.
 *
 * The percentile computation uses the nearest-rank method, matching the
 * implementation in Google's `com.google.api.gax.core.Distribution` class.
 * Google's `MessageDispatcher` uses a 99.9th percentile by default; this
 * class defaults to the same.
 *
 * Pass the same instance to `autoExtendAckDeadlines` (which reads the computed
 * deadline) and to the acknowledge/nack operators (which record completion times).
 *
 * Usage:
 * {{{
 * val dist = AckDeadlineDistribution(
 *   initialDeadlineSeconds = 10,
 *   minDeadlineSeconds = 10,
 *   maxDeadlineSeconds = 600)
 *
 * GooglePubSub.subscribe(request, 1.second)
 *   .via(GooglePubSub.autoExtendAckDeadlines(subscriptionFqrs, 3.seconds, dist))
 *   .mapAsync(4)(processMessage)
 *   .map(msg => AcknowledgeRequest(sub, Seq(msg.ackId)))
 *   .to(GooglePubSub.acknowledge(parallelism = 1, dist))
 * }}}
 *
 * @since 2.0.0
 */
@ApiMayChange
final class AckDeadlineDistribution private (
    val initialDeadlineSeconds: Int,
    val minDeadlineSeconds: Int,
    val maxDeadlineSeconds: Int,
    val maxAckExtensionPeriodNanos: Long,
    val percentile: Double) {

  require(minDeadlineSeconds >= AckDeadlineDistribution.MinStreamAckDeadlineSeconds,
    s"minDeadlineSeconds must be >= ${AckDeadlineDistribution.MinStreamAckDeadlineSeconds} (Pub/Sub minimum)")
  require(maxDeadlineSeconds <= AckDeadlineDistribution.MaxStreamAckDeadlineSeconds,
    s"maxDeadlineSeconds must be <= ${AckDeadlineDistribution.MaxStreamAckDeadlineSeconds} (Pub/Sub maximum)")
  require(minDeadlineSeconds <= maxDeadlineSeconds, "minDeadlineSeconds must be <= maxDeadlineSeconds")
  require(percentile > 0.0 && percentile <= 100.0, "percentile must be between 0 (exclusive) and 100 (inclusive)")

  // Tracks when each ackId was first seen (entry nanos). Uses java.lang.Long for null-safety with ConcurrentHashMap.
  private[grpc] val deliveryTimes = new ConcurrentHashMap[String, java.lang.Long]()

  // Histogram using AtomicLongArray — matches Google's com.google.api.gax.core.Distribution.
  // Buckets cover 0 (inclusive) to endValue (exclusive) where endValue = maxDeadlineSeconds + 1.
  // Values >= endValue are clamped to endValue - 1 (long-tail handling).
  private val endValue = maxDeadlineSeconds + 1
  private val buckets = new AtomicLongArray(endValue)
  private val count = new AtomicInteger(0)

  /** Record that a message was delivered (start tracking). */
  private[grpc] def recordDelivery(ackId: String): Unit =
    deliveryTimes.put(ackId, java.lang.Long.valueOf(System.nanoTime()))

  /**
   * Record that a message was acked/nacked (stop tracking, record latency).
   * Latency is recorded in seconds, rounded up via `Math.ceil`.
   * This matches Google's `MessageDispatcher`:
   * {{{
   * int ackLatency = Ints.saturatedCast(
   *     (long) Math.ceil((clock.millisTime() - receivedTimeMillis) / 1000D));
   * ackLatencyDistribution.record(ackLatency);
   * }}}
   */
  private[grpc] def recordCompletion(ackId: String): Unit = {
    val startTime: java.lang.Long = deliveryTimes.remove(ackId)
    if (startTime ne null) {
      val elapsedNanos = System.nanoTime() - startTime.longValue()
      val latencySeconds = math.ceil(elapsedNanos / 1000000000.0).toInt
      record(latencySeconds)
    }
  }

  /** Record completion for multiple ack IDs. */
  private[grpc] def recordCompletions(ackIds: Iterable[String]): Unit =
    ackIds.foreach(recordCompletion)

  /**
   * Record a value into the distribution.
   * Mirrors `com.google.api.gax.core.Distribution.record(int)`:
   * values >= endValue are clamped to endValue - 1.
   */
  private def record(value: Int): Unit = {
    require(value >= 0, "value must be non-negative")
    val clamped = if (value >= endValue) endValue - 1 else value
    buckets.incrementAndGet(clamped)
    count.incrementAndGet()
  }

  /**
   * Get the current adaptive deadline in seconds, based on the configured percentile
   * of observed processing times. Returns `initialDeadlineSeconds` if no observations yet.
   *
   * Uses the nearest-rank method, matching `com.google.api.gax.core.Distribution.getPercentile`:
   * {{{
   * long targetRank = (long) Math.ceil(percentile * count.get() / 100);
   * long rank = 0;
   * for (int i = 0; i < buckets.length(); i++) {
   *     rank += buckets.get(i);
   *     if (rank >= targetRank) return i;
   * }
   * return buckets.length();
   * }}}
   *
   * The result is then clamped to `[minDeadlineSeconds, maxDeadlineSeconds]` and further
   * bounded by Pub/Sub's stream ack deadline range (10–600s), matching
   * `MessageDispatcher.computeDeadlineSeconds`.
   */
  def currentDeadlineSeconds: Int = {
    val total = count.get()
    if (total == 0) return initialDeadlineSeconds

    // Nearest-rank method — read count before iterating (intentional low bias on concurrent access)
    val targetRank = math.ceil(percentile * total / 100.0).toLong
    var rank = 0L
    var i = 0
    while (i < buckets.length()) {
      rank += buckets.get(i)
      if (rank >= targetRank) {
        return clampDeadline(i)
      }
      i += 1
    }
    clampDeadline(buckets.length())
  }

  /**
   * Clamp deadline to configured bounds, then to Pub/Sub stream ack deadline range.
   * Matches the clamping logic in `MessageDispatcher.computeDeadlineSeconds`.
   */
  private def clampDeadline(computed: Int): Int = {
    var deadline = computed
    if (deadline > maxDeadlineSeconds) deadline = maxDeadlineSeconds
    if (deadline < minDeadlineSeconds) deadline = minDeadlineSeconds
    if (deadline < AckDeadlineDistribution.MinStreamAckDeadlineSeconds)
      deadline = AckDeadlineDistribution.MinStreamAckDeadlineSeconds
    if (deadline > AckDeadlineDistribution.MaxStreamAckDeadlineSeconds)
      deadline = AckDeadlineDistribution.MaxStreamAckDeadlineSeconds
    deadline
  }

  /**
   * Check if a tracked message has exceeded the max extension period.
   * If so, remove it from tracking and return true.
   */
  private[grpc] def isExpired(ackId: String, now: Long): Boolean = {
    val startTime: java.lang.Long = deliveryTimes.get(ackId)
    if ((startTime ne null) && (now - startTime.longValue()) > maxAckExtensionPeriodNanos) {
      deliveryTimes.remove(ackId)
      true
    } else {
      false
    }
  }
}

@ApiMayChange
object AckDeadlineDistribution {

  /** Pub/Sub minimum stream ack deadline (matches `Subscriber.MIN_STREAM_ACK_DEADLINE`). */
  final val MinStreamAckDeadlineSeconds = 10

  /** Pub/Sub maximum stream ack deadline (matches `Subscriber.MAX_STREAM_ACK_DEADLINE`). */
  final val MaxStreamAckDeadlineSeconds = 600

  /** Default percentile used by Google's `MessageDispatcher` (`PERCENTILE_FOR_ACK_DEADLINE_UPDATES`). */
  final val DefaultPercentile = 99.9

  /** Default initial stream ack deadline (matches `Subscriber.STREAM_ACK_DEADLINE_DEFAULT`). */
  final val DefaultInitialDeadlineSeconds = 60

  /**
   * Create a new [[AckDeadlineDistribution]].
   *
   * @param initialDeadlineSeconds deadline to use before enough data is collected
   *                               (default 60, matching `Subscriber.STREAM_ACK_DEADLINE_DEFAULT`)
   * @param minDeadlineSeconds minimum deadline in seconds (default 10, Pub/Sub minimum)
   * @param maxDeadlineSeconds maximum deadline in seconds (default 600, Pub/Sub maximum)
   * @param maxAckExtensionPeriodSeconds maximum total time to keep extending a message (default 3600 = 60 min)
   * @param percentile percentile of processing time distribution to use
   *                   (default 99.9, matching Google's `PERCENTILE_FOR_ACK_DEADLINE_UPDATES`)
   */
  def apply(
      initialDeadlineSeconds: Int = DefaultInitialDeadlineSeconds,
      minDeadlineSeconds: Int = MinStreamAckDeadlineSeconds,
      maxDeadlineSeconds: Int = MaxStreamAckDeadlineSeconds,
      maxAckExtensionPeriodSeconds: Int = 3600,
      percentile: Double = DefaultPercentile): AckDeadlineDistribution =
    new AckDeadlineDistribution(
      initialDeadlineSeconds,
      minDeadlineSeconds,
      maxDeadlineSeconds,
      maxAckExtensionPeriodSeconds.toLong * 1000000000L,
      percentile)

  /**
   * Java API: Create a new [[AckDeadlineDistribution]].
   */
  def create(
      initialDeadlineSeconds: Int,
      minDeadlineSeconds: Int,
      maxDeadlineSeconds: Int,
      maxAckExtensionPeriodSeconds: Int,
      percentile: Double): AckDeadlineDistribution =
    apply(initialDeadlineSeconds, minDeadlineSeconds, maxDeadlineSeconds, maxAckExtensionPeriodSeconds, percentile)

  /**
   * Java API: Create with defaults matching Google's official client library.
   */
  def create(): AckDeadlineDistribution = apply()
}