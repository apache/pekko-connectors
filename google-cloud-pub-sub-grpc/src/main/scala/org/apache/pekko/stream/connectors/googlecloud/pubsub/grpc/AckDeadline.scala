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

import scala.concurrent.duration._

/**
 * Configuration for how a [[scaladsl.Subscriber]] (or its Java equivalent) extends ack deadlines
 * for messages it has received.
 *
 *  - [[AckDeadline.Fixed]] uses a constant deadline value on every extension. Cheapest, easiest
 *    to reason about. Use this if your processing latency is predictable.
 *  - [[AckDeadline.Adaptive]] adapts the deadline based on observed processing latencies via a
 *    shared [[AckDeadlineDistribution]], matching Google's official client library behavior.
 *    Use this if processing latency varies widely.
 *
 * @since 2.0.0
 */
@ApiMayChange
sealed trait AckDeadline {
  def extensionInterval: FiniteDuration
}

@ApiMayChange
object AckDeadline {

  /**
   * Extend deadlines on a fixed schedule using a constant deadline value.
   *
   * @param extensionInterval how often to extend deadlines (should be less than the deadline)
   * @param deadlineSeconds the new deadline to set on each extension
   * @param maxAckExtensionPeriod maximum total time to keep extending a message's deadline
   *                              (default 60 minutes, matching Google's client library)
   */
  final case class Fixed(
      extensionInterval: FiniteDuration,
      deadlineSeconds: Int,
      maxAckExtensionPeriod: FiniteDuration = 60.minutes) extends AckDeadline

  /**
   * Extend deadlines on a fixed schedule using an adaptive deadline computed from an
   * [[AckDeadlineDistribution]]. The same distribution must be passed to acknowledge/nack
   * operators so that completion latencies are recorded into the histogram.
   */
  final case class Adaptive(
      extensionInterval: FiniteDuration,
      distribution: AckDeadlineDistribution) extends AckDeadline

  /** Java API: fixed-deadline configuration with a default `maxAckExtensionPeriod` of 60 minutes. */
  def fixed(extensionInterval: java.time.Duration, deadlineSeconds: Int): AckDeadline =
    Fixed(FiniteDuration(extensionInterval.toNanos, NANOSECONDS), deadlineSeconds)

  /** Java API: fixed-deadline configuration with an explicit `maxAckExtensionPeriod`. */
  def fixed(extensionInterval: java.time.Duration, deadlineSeconds: Int,
      maxAckExtensionPeriod: java.time.Duration): AckDeadline =
    Fixed(FiniteDuration(extensionInterval.toNanos, NANOSECONDS), deadlineSeconds,
      FiniteDuration(maxAckExtensionPeriod.toNanos, NANOSECONDS))

  /** Java API: adaptive-deadline configuration. */
  def adaptive(extensionInterval: java.time.Duration,
      distribution: AckDeadlineDistribution): AckDeadline =
    Adaptive(FiniteDuration(extensionInterval.toNanos, NANOSECONDS), distribution)
}
