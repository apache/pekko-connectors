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

package org.apache.pekko.stream.connectors.jms

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.pekko.stream.connectors.jms.impl.{ JmsAckSession, JmsSession }
import javax.jms

import scala.concurrent.{ Future, Promise }

case class AckEnvelope private[jms] (message: jms.Message, private val jmsSession: JmsAckSession) {

  val processed = new AtomicBoolean(false)

  def acknowledge(): Unit = if (processed.compareAndSet(false, true)) jmsSession.ack(message)
}

case class TxEnvelope private[jms] (message: jms.Message, private val jmsSession: JmsSession) {

  private[this] val commitPromise = Promise[() => Unit]()

  private[jms] val commitFuture: Future[() => Unit] = commitPromise.future

  def commit(): Unit = commitPromise.success(jmsSession.session.commit _)

  def rollback(): Unit = commitPromise.success(jmsSession.session.rollback _)
}
