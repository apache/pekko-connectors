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

package org.apache.pekko.stream.connectors.geode.impl.stage

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream._
import pekko.stream.connectors.geode.RegionSettings
import pekko.stream.connectors.geode.impl.GeodeCapabilities
import pekko.stream.stage._
import org.apache.geode.cache.client.ClientCache

@InternalApi
private[geode] class GeodeFlowStage[K, T <: AnyRef](cache: ClientCache, settings: RegionSettings[K, T])
    extends GraphStage[FlowShape[T, T]] {

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name("GeodeFlow") and ActorAttributes.IODispatcher

  private val in = Inlet[T]("geode.in")
  private val out = Outlet[T]("geode.out")

  override val shape: FlowShape[T, T] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging with GeodeCapabilities[K, T] {

      override protected def logSource: Class[GeodeFlowStage[K, T]] = classOf[GeodeFlowStage[K, T]]

      val regionSettings: RegionSettings[K, T] = settings

      val clientCache: ClientCache = cache

      setHandler(out,
        new OutHandler {
          override def onPull(): Unit =
            pull(in)
        })

      setHandler(in,
        new InHandler {
          override def onPush(): Unit = {
            val msg = grab(in)

            put(msg)

            push(out, msg)
          }

        })

      override def postStop(): Unit = {
        log.debug("Stage completed")
        close()
      }
    }

}
