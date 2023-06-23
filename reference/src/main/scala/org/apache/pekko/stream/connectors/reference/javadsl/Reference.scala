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

package org.apache.pekko.stream.connectors.reference.javadsl

import java.util.concurrent.{ CompletionStage, Executor }

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.stream.connectors.reference.scaladsl
import pekko.stream.connectors.reference.{
  ReferenceReadResult,
  ReferenceWriteMessage,
  ReferenceWriteResult,
  SourceSettings
}
import pekko.stream.javadsl.{ Flow, Source }

import scala.concurrent.ExecutionContext

object Reference {

  /**
   * No Java API at the start of the method doc needed, since the package is dedicated to the Java API.
   *
   * Call Scala source factory and convert both: the source and materialized values to Java classes.
   */
  def source(settings: SourceSettings): Source[ReferenceReadResult, CompletionStage[Done]] = {
    import org.apache.pekko.util.FutureConverters._
    scaladsl.Reference.source(settings).mapMaterializedValue(_.asJava).asJava
  }

  /**
   * Only convert the flow type, as the materialized value type is the same between Java and Scala.
   */
  def flow(): Flow[ReferenceWriteMessage, ReferenceWriteResult, NotUsed] =
    scaladsl.Reference.flow().asJava

  /**
   * In Java API take Executor as parameter if the operator needs to perform asynchronous tasks.
   */
  def flowAsyncMapped(ex: Executor): Flow[ReferenceWriteMessage, ReferenceWriteResult, NotUsed] =
    scaladsl.Reference.flowAsyncMapped()(ExecutionContext.fromExecutor(ex)).asJava

  /**
   * An implementation of a flow that needs access to materializer or attributes during materialization.
   */
  def flowWithResource(): Flow[ReferenceWriteMessage, ReferenceWriteResult, NotUsed] =
    scaladsl.Reference.flowWithResource().asJava
}
