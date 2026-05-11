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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream._
import org.apache.pekko.stream.stage._

/**
 * INTERNAL API
 *
 * A passthrough GraphStage that eagerly pulls from upstream into a bounded internal buffer
 * and invokes `onTrack` the moment an element is grabbed — before downstream demand is required.
 *
 * Motivation: Pub/Sub `StreamingPull` starts the server-side ack deadline timer the moment a
 * message is dispatched to the client. With a plain `.map { tracked.put(...); identity }`,
 * tracking only fires when downstream pulls — so under backpressure (e.g. saturated `mapAsync`),
 * messages buffer in the gRPC adapter / `mapConcat` with deadlines ticking but no client-side
 * tracking, leaving them unprotected from the auto-extend ticker.
 *
 * This stage decouples receipt-side tracking from downstream demand. When the buffer is full
 * the stage stops pulling, applying backpressure upstream — which (combined with server-side
 * `StreamingPullRequest.maxOutstandingMessages`) bounds memory.
 *
 * Mirrors the pattern in Google's `MessageDispatcher.processReceivedMessages`, which registers
 * messages in `pendingMessages` before handing them to user callbacks.
 */
@InternalApi
private[grpc] final class EagerPullTrackingStage[T](maxBuffer: Int, onTrack: T => Unit)
    extends GraphStage[FlowShape[T, T]] {
  require(maxBuffer > 0, "maxBuffer must be > 0")

  val in: Inlet[T] = Inlet("EagerPullTracking.in")
  val out: Outlet[T] = Outlet("EagerPullTracking.out")
  override val shape: FlowShape[T, T] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      // Pre-size to maxBuffer so the dynamic-array doesn't have to resize during initial fill.
      // Buffer fill is bounded by maxBuffer via pullIfPossible, so this is also the high water mark.
      private val buffer = new java.util.ArrayDeque[T](maxBuffer)

      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        val msg = grab(in)
        onTrack(msg)
        buffer.offer(msg)
        pushIfPossible()
        pullIfPossible()
      }

      override def onPull(): Unit = {
        pushIfPossible()
        pullIfPossible()
      }

      override def onUpstreamFinish(): Unit =
        if (buffer.isEmpty) completeStage()
      // else: drain via subsequent onPull invocations

      override def onUpstreamFailure(ex: Throwable): Unit = {
        buffer.clear()
        super.onUpstreamFailure(ex)
      }

      private def pushIfPossible(): Unit =
        if (isAvailable(out) && !buffer.isEmpty) push(out, buffer.poll())

      private def pullIfPossible(): Unit =
        if (buffer.size < maxBuffer && !hasBeenPulled(in) && !isClosed(in)) pull(in)
        else if (buffer.isEmpty && isClosed(in)) completeStage()

      setHandlers(in, out, this)
    }
}
