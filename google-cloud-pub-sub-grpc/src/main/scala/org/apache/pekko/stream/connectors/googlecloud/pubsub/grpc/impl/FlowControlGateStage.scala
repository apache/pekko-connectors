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
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.FlowControl

/**
 * INTERNAL API
 *
 * A GraphStage that gates elements through based on a shared [[FlowControl]] counter using an
 * eager-pull strategy: elements are pulled from upstream and counted against the permit limit
 * the moment they arrive, independent of downstream demand. When the permit limit is reached
 * the stage stops pulling, applying backpressure upstream all the way to the gRPC adapter.
 *
 * Combined with the connector's gRPC backpressure, this transitively bounds how many messages
 * the Pub/Sub server is allowed to deliver, giving you flow control with semantics close to
 * Google's `FlowController` rather than just downstream credit accounting.
 *
 * Internally the stage holds a small buffer for elements that have been received and counted
 * but not yet pushed downstream. The buffer is naturally bounded by the FlowControl limit, so
 * no separate maxBuffer parameter is needed.
 *
 * When downstream acknowledges or nacks via the same [[FlowControl]], the `onRelease` callback
 * triggers a re-evaluation of whether to pull more.
 */
@InternalApi
private[grpc] final class FlowControlGateStage[T](flowControl: FlowControl)
    extends GraphStage[FlowShape[T, T]] {

  val in: Inlet[T] = Inlet("FlowControlGate.in")
  val out: Outlet[T] = Outlet("FlowControlGate.out")
  override val shape: FlowShape[T, T] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      // Pre-size to the FlowControl limit (clamped to a sane Int) so the dynamic array doesn't
      // resize during initial fill. Fill is bounded by the limit via pullIfPossible, so this is
      // the high water mark for typical configurations. The clamp prevents a pathological
      // initial-allocation when callers set maxOutstandingMessages near Long.MaxValue.
      private val buffer =
        new java.util.ArrayDeque[T](math.min(flowControl.maxOutstandingMessages, 65536L).toInt)

      private val releaseCallback: AsyncCallback[Unit] = getAsyncCallback[Unit] { _ =>
        pullIfPossible()
      }

      override def preStart(): Unit = {
        flowControl.onRelease = () => releaseCallback.invoke(())
        pull(in)
      }

      override def postStop(): Unit = {
        flowControl.onRelease = () => ()
      }

      override def onPush(): Unit = {
        val msg = grab(in)
        flowControl.acquire() // count on receipt, not on emission to downstream
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
        if (flowControl.outstanding.get() < flowControl.maxOutstandingMessages
          && !hasBeenPulled(in) && !isClosed(in)) pull(in)
        else if (buffer.isEmpty && isClosed(in)) completeStage()

      setHandlers(in, out, this)
    }
}
