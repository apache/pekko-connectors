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

package org.apache.pekko.stream.connectors.jakartams.impl

import jakarta.jms
import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.jakartams.{ Destination, DurableTopic }
import pekko.util.OptionVal

import java.util.concurrent.ArrayBlockingQueue
import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Internal API.
 */
@InternalApi
private[jakartams] sealed trait JmsSession {

  def connection: jms.Connection

  def session: jms.Session

  private[jakartams] def abortSession(): Unit = closeSession()

  private[jakartams] def closeSession(): Unit = session.close()
}

/**
 * Internal API.
 */
@InternalApi
private[jakartams] final class JmsProducerSession(val connection: jms.Connection,
    val session: jms.Session,
    val jmsDestination: jms.Destination)
    extends JmsSession

/**
 * Internal API.
 */
@InternalApi
private[jakartams] class JmsConsumerSession(val connection: jms.Connection,
    val session: jms.Session,
    val jmsDestination: jms.Destination,
    val settingsDestination: Destination)
    extends JmsSession {

  private[jakartams] def createConsumer(
      selector: Option[String])(implicit ec: ExecutionContext): Future[jms.MessageConsumer] =
    Future {
      (selector, settingsDestination) match {
        case (None, t: DurableTopic) =>
          session.createDurableSubscriber(jmsDestination.asInstanceOf[jms.Topic], t.subscriberName)

        case (Some(expr), t: DurableTopic) =>
          session.createDurableSubscriber(jmsDestination.asInstanceOf[jms.Topic], t.subscriberName, expr, false)

        case (Some(expr), _) =>
          session.createConsumer(jmsDestination, expr)

        case (None, _) =>
          session.createConsumer(jmsDestination)
      }
    }
}

case object SessionClosed

/**
 * Internal API.
 */
@InternalApi
private[jakartams] final class JmsAckSession(override val connection: jms.Connection,
    override val session: jms.Session,
    override val jmsDestination: jms.Destination,
    override val settingsDestination: Destination,
    val maxPendingAcks: Int)
    extends JmsConsumerSession(connection, session, jmsDestination, settingsDestination) {

  private val ackQueue = new ArrayBlockingQueue[Either[SessionClosed.type, () => Unit]](maxPendingAcks + 1)
  private[jakartams] var pendingAck = 0
  private var listenerRunning = true

  def isListenerRunning: Boolean = listenerRunning

  def maxPendingAcksReached: Boolean = pendingAck > maxPendingAcks

  def ack(message: jms.Message): Unit = ackQueue.put(Right(message.acknowledge _))

  override def closeSession(): Unit = stopMessageListenerAndCloseSession()

  override def abortSession(): Unit = stopMessageListenerAndCloseSession()

  private def stopMessageListenerAndCloseSession(): Unit = {
    try {
      drainAcks()
    } finally {
      ackQueue.put(Left(SessionClosed))
      session.close()
    }
  }

  def ackBackpressure() = {
    ackQueue.take() match {
      case Left(SessionClosed) =>
        listenerRunning = false
      case Right(action) =>
        action()
        pendingAck -= 1
    }
  }

  @tailrec
  def drainAcks(): Unit =
    OptionVal(ackQueue.poll()) match {
      case OptionVal.Some(Left(SessionClosed)) =>
        listenerRunning = false
      case OptionVal.Some(Right(action)) =>
        action()
        pendingAck -= 1
        drainAcks()
      case OptionVal.None =>
      case other          => throw new MatchError(other)
    }
}
