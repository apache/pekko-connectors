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

package org.apache.pekko.stream.connectors.jms.impl

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.jms.impl.InternalConnectionState.JmsConnectorStopping
import org.apache.pekko.stream.connectors.jms.{ Destination, JmsConsumerSettings }
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.stage.{ OutHandler, StageLogging, TimerGraphStageLogic }
import org.apache.pekko.stream.{ Attributes, Outlet, SourceShape }
import org.apache.pekko.{ Done, NotUsed }

import scala.collection.mutable
import scala.util.{ Failure, Success }

import javax.jms

/**
 * Internal API.
 */
@InternalApi
private trait JmsConsumerConnector extends JmsConnector[JmsConsumerSession] {
  this: TimerGraphStageLogic with StageLogging =>

  override val startConnection = true

  protected def createSession(connection: jms.Connection,
      createDestination: jms.Session => jms.Destination): JmsConsumerSession

}

/**
 * Internal API.
 */
@InternalApi
private abstract class SourceStageLogic[T](shape: SourceShape[T],
    out: Outlet[T],
    settings: JmsConsumerSettings,
    val destination: Destination,
    inheritedAttributes: Attributes)
    extends TimerGraphStageLogic(shape)
    with JmsConsumerConnector
    with StageLogging {

  override protected def jmsSettings: JmsConsumerSettings = settings
  private val queue = mutable.Queue[T]()
  private val stopping = new AtomicBoolean(false)
  private var stopped = false

  private val markStopped = getAsyncCallback[Done.type] { _ =>
    stopped = true
    finishStop()
    if (queue.isEmpty) completeStage()
  }

  private val markAborted = getAsyncCallback[Throwable] { ex =>
    stopped = true
    finishStop()
    failStage(ex)
  }

  protected val handleError = getAsyncCallback[Throwable] { e =>
    updateState(JmsConnectorStopping(Failure(e)))
    failStage(e)
  }

  override def preStart(): Unit = {
    ec = executionContext(inheritedAttributes)
    super.preStart()
    initSessionAsync()
  }

  private[jms] val handleMessage = getAsyncCallback[T] { msg =>
    if (isAvailable(out)) {
      if (queue.isEmpty) {
        pushMessage(msg)
      } else {
        pushMessage(queue.dequeue())
        queue.enqueue(msg)
      }
    } else {
      queue.enqueue(msg)
    }
  }

  protected def pushMessage(msg: T): Unit

  setHandler(
    out,
    new OutHandler {
      override def onPull(): Unit = {
        if (queue.nonEmpty) pushMessage(queue.dequeue())
        if (stopped && queue.isEmpty) completeStage()
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
        // no need to keep messages in the queue, downstream will never pull them.
        queue.clear()
        // keep processing async callbacks for stopSessions.
        setKeepGoing(true)
        stopSessions()
      }
    })

  private def stopSessions(): Unit =
    if (stopping.compareAndSet(false, true)) {
      val status = updateState(JmsConnectorStopping(Success(Done)))
      val connectionFuture = JmsConnector.connection(status)

      closeSessionsAsync().onComplete { _ =>
        closeConnectionAsync(connectionFuture).onComplete { _ =>
          // By this time, after stopping connection, closing sessions, all async message submissions to this
          // stage should have been invoked. We invoke markStopped as the last item so it gets delivered after
          // all JMS messages are delivered. This will allow the stage to complete after all pending messages
          // are delivered, thus preventing message loss due to premature stage completion.
          markStopped.invoke(Done)
        }
      }
    }

  private def abortSessions(ex: Throwable): Unit =
    if (stopping.compareAndSet(false, true)) {
      if (log.isDebugEnabled) log.debug("aborting sessions ({})", ex.toString)
      val status = updateState(JmsConnectorStopping(Failure(ex)))
      val connectionFuture = JmsConnector.connection(status)
      abortSessionsAsync().onComplete { _ =>
        closeConnectionAsync(connectionFuture).onComplete { _ =>
          markAborted.invoke(ex)
        }
      }
    }

  def consumerControl: JmsConsumerMatValue = new JmsConsumerMatValue {
    override def shutdown(): Unit = stopSessions()
    override def abort(ex: Throwable): Unit = abortSessions(ex)
    override def connected: Source[InternalConnectionState, NotUsed] =
      Source.future(connectionStateSource).flatMapConcat(identity)
  }

  override def postStop(): Unit = finishStop()
}
