/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.reference.impl

import org.apache.pekko
import pekko.Done
import pekko.annotation.InternalApi
import pekko.event.Logging
import pekko.stream._
import pekko.stream.connectors.reference.{ ReferenceReadResult, SourceSettings }
import pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, OutHandler }
import pekko.util.ByteString

import scala.collection.immutable
import scala.concurrent.{ Future, Promise }
import scala.util.Success

/**
 * INTERNAL API
 *
 * Private package hides the class from the API in Scala. However it is still
 * visible in Java. Use "InternalApi" annotation and "INTERNAL API" as the first
 * line in scaladoc to communicate to Java developers that this is private API.
 */
@InternalApi private[reference] final class ReferenceSourceStageLogic(
    val settings: SourceSettings,
    val startupPromise: Promise[Done],
    val shape: SourceShape[ReferenceReadResult]) extends GraphStageLogic(shape) {

  private def out = shape.out

  /**
   * Initialization logic
   */
  override def preStart(): Unit =
    startupPromise.success(Done)

  setHandler(out,
    new OutHandler {
      override def onPull(): Unit = push(
        out,
        new ReferenceReadResult(immutable.Seq(ByteString("one")), Success(100)))
    })

  /**
   * Cleanup logic
   */
  override def postStop(): Unit = {}
}

/**
 * INTERNAL API
 */
@InternalApi private[reference] final class ReferenceSourceStage(settings: SourceSettings)
    extends GraphStageWithMaterializedValue[SourceShape[ReferenceReadResult], Future[Done]] {
  val out: Outlet[ReferenceReadResult] = Outlet(Logging.simpleName(this) + ".out")

  override def initialAttributes: Attributes =
    Attributes.name(Logging.simpleName(this))

  override val shape: SourceShape[ReferenceReadResult] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    // materialized value created as a new instance on every materialization
    val startupPromise = Promise[Done]()
    val logic = new ReferenceSourceStageLogic(settings, startupPromise, shape)
    (logic, startupPromise.future)
  }

}
