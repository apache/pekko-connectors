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
import pekko.Done
import pekko.annotation.InternalApi
import pekko.stream.stage._
import pekko.stream.{ ActorAttributes, Attributes, Outlet, SourceShape }
import org.apache.geode.cache.client.ClientCache

import scala.concurrent.{ Future, Promise }

@InternalApi
private[geode] class GeodeFiniteSourceStage[V](cache: ClientCache, sql: String)
    extends GraphStageWithMaterializedValue[SourceShape[V], Future[Done]] {

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name("GeodeFiniteSource") and ActorAttributes.IODispatcher

  val out = Outlet[V]("geode.finiteSource")

  override def shape: SourceShape[V] = SourceShape.of(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val subPromise = Promise[Done]()

    (new GeodeQueryGraphLogic[V](shape, cache, sql) {

        override val onConnect: AsyncCallback[Unit] = getAsyncCallback[Unit] { _ =>
          subPromise.success(Done)
        }

        setHandler(
          out,
          new OutHandler {
            override def onPull(): Unit =
              if (initialResultsIterator.hasNext)
                push(out, initialResultsIterator.next())
              else
                completeStage()
          })

      }, subPromise.future)
  }
}
