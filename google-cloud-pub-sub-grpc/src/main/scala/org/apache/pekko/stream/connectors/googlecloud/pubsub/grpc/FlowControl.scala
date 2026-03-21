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

import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks the number of outstanding (unacknowledged) messages and provides
 * a mechanism for the downstream acknowledge/nack stages to signal that
 * messages have been processed, allowing the upstream gate to resume pulling.
 *
 * Create an instance and pass it to both `flowControlGate` (which applies
 * backpressure) and `acknowledge`/`nack` overloads (which release permits).
 *
 * Usage:
 * {{{
 * val fc = FlowControl(maxOutstandingMessages = 1000)
 *
 * GooglePubSub.subscribe(request, 1.second)
 *   .via(GooglePubSub.flowControlGate(fc))
 *   .mapAsync(4)(processMessage)
 *   .map(msg => AcknowledgeRequest(sub, Seq(msg.ackId)))
 *   .to(GooglePubSub.acknowledge(parallelism = 1, fc))
 * }}}
 *
 * @param maxOutstandingMessages maximum number of messages allowed in-flight
 * @since 2.0.0
 */
@ApiMayChange
final class FlowControl(val maxOutstandingMessages: Long) {
  require(maxOutstandingMessages > 0, "maxOutstandingMessages must be > 0")

  private[grpc] val outstanding = new AtomicLong(0)

  @volatile private[grpc] var onRelease: () => Unit = () => ()

  /** Current number of outstanding (unacknowledged) messages. */
  def outstandingCount: Long = outstanding.get()

  /** Release `count` permits, signalling that messages have been acknowledged. */
  private[grpc] def release(count: Int): Unit = {
    outstanding.addAndGet(-count)
    onRelease()
  }

  /** Acquire one permit. Returns true if under the limit. */
  private[grpc] def acquire(): Boolean =
    outstanding.incrementAndGet() <= maxOutstandingMessages
}

@ApiMayChange
object FlowControl {

  /** Create a new [[FlowControl]] instance. */
  def apply(maxOutstandingMessages: Long): FlowControl =
    new FlowControl(maxOutstandingMessages)

  /** Java API: Create a new [[FlowControl]] instance. */
  def create(maxOutstandingMessages: Long): FlowControl =
    new FlowControl(maxOutstandingMessages)
}