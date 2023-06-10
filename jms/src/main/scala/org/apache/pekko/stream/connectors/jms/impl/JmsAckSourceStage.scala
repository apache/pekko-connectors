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

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.jms._
import pekko.stream.connectors.jms.impl.JmsConnector.FlushAcknowledgementsTimerKey
import pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue }
import pekko.stream.{ Attributes, Outlet, SourceShape }
import javax.jms

/**
 * Internal API.
 */
@InternalApi
private[jms] final class JmsAckSourceStage(settings: JmsConsumerSettings, destination: Destination)
    extends GraphStageWithMaterializedValue[SourceShape[AckEnvelope], JmsConsumerMatValue] {

  private val out = Outlet[AckEnvelope]("JmsSource.out")

  override def shape: SourceShape[AckEnvelope] = SourceShape[AckEnvelope](out)

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, JmsConsumerMatValue) = {
    val logic = new JmsAckSourceStageLogic(inheritedAttributes)
    (logic, logic.consumerControl)
  }

  override protected def initialAttributes: Attributes = Attributes.name("JmsAckConsumer")

  private final class JmsAckSourceStageLogic(inheritedAttributes: Attributes)
      extends SourceStageLogic[AckEnvelope](shape, out, settings, destination, inheritedAttributes) {
    private val maxPendingAcks = settings.maxPendingAcks
    private val maxAckInterval = settings.maxAckInterval

    protected def createSession(connection: jms.Connection,
        createDestination: jms.Session => javax.jms.Destination): JmsAckSession = {
      val session =
        connection.createSession(false, settings.acknowledgeMode.getOrElse(AcknowledgeMode.ClientAcknowledge).mode)
      new JmsAckSession(connection, session, createDestination(session), graphStageDestination, maxPendingAcks)
    }

    protected def pushMessage(msg: AckEnvelope): Unit = push(out, msg)

    override protected def onSessionOpened(jmsSession: JmsConsumerSession): Unit =
      jmsSession match {
        case session: JmsAckSession =>
          maxAckInterval.foreach { timeout =>
            scheduleWithFixedDelay(FlushAcknowledgementsTimerKey(session), timeout, timeout)
          }
          session
            .createConsumer(settings.selector)
            .map { consumer =>
              consumer.setMessageListener((message: jms.Message) => {
                if (session.isListenerRunning)
                  try {
                    handleMessage.invoke(AckEnvelope(message, session))
                    session.pendingAck += 1
                    if (session.maxPendingAcksReached) {
                      session.ackBackpressure()
                    }
                    session.drainAcks()
                  } catch {
                    case e: jms.JMSException =>
                      handleError.invoke(e)
                  }
              })
            }
            .onComplete(sessionOpenedCB.invoke)

        case _ =>
          throw new IllegalArgumentException(
            "Session must be of type JMSAckSession, it is a " +
            jmsSession.getClass.getName)
      }
  }

}
