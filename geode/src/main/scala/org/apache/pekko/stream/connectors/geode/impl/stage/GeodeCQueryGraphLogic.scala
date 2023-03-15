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

package org.apache.pekko.stream.connectors.geode.impl.stage

import java.util
import java.util.concurrent.Semaphore

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.stage.{ AsyncCallback, StageLogging }
import pekko.stream.{ Outlet, SourceShape }
import org.apache.geode.cache.client.ClientCache
import org.apache.geode.cache.query.{ CqAttributesFactory, CqEvent, CqQuery, Struct }
import org.apache.geode.cache.util.CqListenerAdapter

import scala.collection.mutable
import scala.util.Try

@InternalApi
private[geode] abstract class GeodeCQueryGraphLogic[V](val shape: SourceShape[V],
    val clientCache: ClientCache,
    val queryName: String,
    val sql: String)
    extends GeodeSourceStageLogic[V](shape, clientCache)
    with StageLogging {

  /**
   * Queue containing, only
   */
  private val incomingQueue = mutable.Queue[V]()

  private val semaphore = new Semaphore(10)

  val onElement: AsyncCallback[V]

  private var query: CqQuery = _

  override def executeQuery() = Try {

    val cqf = new CqAttributesFactory()

    val eventListener = new CqListenerAdapter() {
      override def onEvent(ev: CqEvent): Unit =
        onGeodeElement(ev.getNewValue().asInstanceOf[V])

      override def onError(ev: CqEvent): Unit =
        log.error(ev.getThrowable, s"$ev")

      override def close(): Unit = {
        log.debug("closes")
        inFinish.invoke(())
      }
    }
    cqf.addCqListener(eventListener)

    val cqa = cqf.create()

    query = qs.newCq(queryName, sql, cqa)

    buildInitialResulsIterator(query)

  }

  private def buildInitialResulsIterator(q: CqQuery) = {
    val res = q.executeWithInitialResults[Struct]
    val it = res.iterator()
    new util.Iterator[V] {
      override def next(): V =
        it.next().getFieldValues()(1).asInstanceOf[V]

      override def hasNext: Boolean = it.hasNext
    }
  }

  /**
   * May lock on semaphore.acquires().
   */
  protected def onGeodeElement(v: V): Unit = {
    semaphore.acquire()
    onElement.invoke(v)
  }

  protected def incomingQueueIsEmpty = incomingQueue.isEmpty

  protected def enqueue(v: V): Unit =
    incomingQueue.enqueue(v)

  protected def dequeue(): Option[V] =
    if (incomingQueue.isEmpty)
      None
    else
      Some(incomingQueue.dequeue())

  /**
   * Pushes an element downstream and releases a semaphore acquired in onGeodeElement.
   */
  protected def pushElement(out: Outlet[V], element: V) = {
    push(out, element)
    semaphore.release()
  }

  override def postStop(): Unit = {
    if (clientCache.isClosed)
      return
    qs.closeCqs()
  }

  /**
   * Geode upstream is terminated.
   */
  @volatile
  private var upstreamTerminated = false

  val inFinish: AsyncCallback[Unit] = getAsyncCallback[Unit] { v =>
    upstreamTerminated = true
    handleTerminaison()
  }

  def handleTerminaison() =
    if (upstreamTerminated && incomingQueue.isEmpty)
      completeStage()

}
