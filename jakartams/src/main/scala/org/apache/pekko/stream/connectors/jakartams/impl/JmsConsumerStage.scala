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
import pekko.stream._
import pekko.stream.connectors.jakartams._
import pekko.stream.stage._

import java.util.concurrent.Semaphore

/**
 * Internal API.
 */
@InternalApi
private[jakartams] final class JmsConsumerStage(settings: JmsConsumerSettings, destination: Destination)
    extends GraphStageWithMaterializedValue[SourceShape[jms.Message], JmsConsumerMatValue] {

  private val out = Outlet[jms.Message]("JmsConsumer.out")

  override protected def initialAttributes: Attributes = Attributes.name("JmsConsumer")

  override def shape: SourceShape[jms.Message] = SourceShape[jms.Message](out)

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, JmsConsumerMatValue) = {
    val logic = new JmsConsumerStageLogic(inheritedAttributes)
    (logic, logic.consumerControl)
  }

  private final class JmsConsumerStageLogic(inheritedAttributes: Attributes)
      extends SourceStageLogic[jms.Message](shape, out, settings, destination, inheritedAttributes) {

    private val bufferSize = (settings.bufferSize + 1) * settings.sessionCount

    private val backpressure = new Semaphore(bufferSize)

    protected def createSession(connection: jms.Connection,
        createDestination: jms.Session => jakarta.jms.Destination): JmsConsumerSession = {
      val session =
        connection.createSession(false, settings.acknowledgeMode.getOrElse(AcknowledgeMode.AutoAcknowledge).mode)
      new JmsConsumerSession(connection, session, createDestination(session), graphStageDestination)
    }

    protected def pushMessage(msg: jms.Message): Unit = {
      push(out, msg)
      backpressure.release()
    }

    override protected def onSessionOpened(jmsSession: JmsConsumerSession): Unit =
      jmsSession
        .createConsumer(settings.selector)
        .map { consumer =>
          consumer.setMessageListener(new jms.MessageListener {
            def onMessage(message: jms.Message): Unit = {
              backpressure.acquire()
              handleMessage.invoke(message)
            }
          })
        }
        .onComplete(sessionOpenedCB.invoke)
  }
}
