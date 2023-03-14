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

package org.apache.pekko.stream.connectors.reference.scaladsl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Attributes
import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.stream.connectors.reference.impl.{
  ReferenceFlowStage,
  ReferenceSourceStage,
  ReferenceWithResourceFlowStage
}
import org.apache.pekko.stream.connectors.reference._
import org.apache.pekko.stream.scaladsl.{ Flow, Source }

import scala.concurrent.{ ExecutionContext, Future }

object Reference {

  /**
   * API doc should describe where the messages are coming from.
   *
   * Also describe the significance of the materialized value.
   */
  def source(settings: SourceSettings): Source[ReferenceReadResult, Future[Done]] =
    Source.fromGraph(new ReferenceSourceStage(settings))

  /**
   * API doc should describe what will be done to the incoming messages to the flow,
   * and what messages will be emitted by the flow.
   */
  def flow(): Flow[ReferenceWriteMessage, ReferenceWriteResult, NotUsed] =
    Flow.fromGraph(new ReferenceFlowStage())

  /**
   * If the operator needs an ExecutionContext, take it as an implicit parameter.
   */
  def flowAsyncMapped()(implicit ec: ExecutionContext): Flow[ReferenceWriteMessage, ReferenceWriteResult, NotUsed] =
    flow().mapAsync(parallelism = 4)(m => Future { m })

  /**
   * An implementation of a flow that needs access to materializer or attributes during materialization.
   */
  def flowWithResource(): Flow[ReferenceWriteMessage, ReferenceWriteResult, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow.fromGraph(new ReferenceWithResourceFlowStage(resolveResource(mat.system, attr)))
      }
      .mapMaterializedValue(_ => NotUsed)

  private def resolveResource(sys: ActorSystem, attr: Attributes) =
    attr.get[ReferenceResourceValue].map(_.resource).getOrElse(ResourceExt()(sys).resource)
}
