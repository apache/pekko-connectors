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

package akka.stream.alpakka.geode.impl.stage

import akka.annotation.InternalApi
import akka.stream.SourceShape
import akka.stream.stage.{ AsyncCallback, GraphStageLogic }
import org.apache.geode.cache.client.ClientCache

import scala.util.{ Failure, Success, Try }

@InternalApi
private[geode] abstract class GeodeSourceStageLogic[V](shape: SourceShape[V], clientCache: ClientCache)
    extends GraphStageLogic(shape) {

  protected var initialResultsIterator: java.util.Iterator[V] = _

  val onConnect: AsyncCallback[Unit]

  lazy val qs = clientCache.getQueryService()

  def executeQuery(): Try[java.util.Iterator[V]]

  final override def preStart(): Unit = executeQuery() match {
    case Success(it) =>
      initialResultsIterator = it
      onConnect.invoke(())
    case Failure(e) =>
      failStage(e)

  }
}
