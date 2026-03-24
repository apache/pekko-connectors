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
 * A GraphStage that gates elements through based on a shared [[FlowControl]] counter.
 * It pulls from upstream only when the number of outstanding messages is below the limit.
 * When downstream acknowledges/nacks via the same [[FlowControl]], the `onRelease` callback
 * triggers a re-evaluation of whether to pull.
 */
@InternalApi
private[grpc] final class FlowControlGateStage[T](flowControl: FlowControl)
    extends GraphStage[FlowShape[T, T]] {

  val in: Inlet[T] = Inlet("FlowControlGate.in")
  val out: Outlet[T] = Outlet("FlowControlGate.out")
  override val shape: FlowShape[T, T] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      private var downstreamWaiting = false

      private val releaseCallback: AsyncCallback[Unit] = getAsyncCallback[Unit] { _ =>
        if (downstreamWaiting && flowControl.outstanding.get() < flowControl.maxOutstandingMessages &&
          !hasBeenPulled(in)) {
          downstreamWaiting = false
          pull(in)
        }
      }

      override def preStart(): Unit = {
        flowControl.onRelease = () => releaseCallback.invoke(())
      }

      override def postStop(): Unit = {
        flowControl.onRelease = () => ()
      }

      // InHandler
      override def onPush(): Unit = {
        val msg = grab(in)
        flowControl.acquire()
        push(out, msg)
      }

      // OutHandler
      override def onPull(): Unit = {
        if (flowControl.outstanding.get() < flowControl.maxOutstandingMessages) {
          pull(in)
        } else {
          downstreamWaiting = true
        }
      }

      setHandlers(in, out, this)
    }
}
