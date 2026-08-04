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

package org.apache.pekko.stream.connectors.jms.impl

import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.jms.{ AcknowledgeMode, Destination, JmsConsumerSettings, JmsTxAckTimeout, TxEnvelope }
import pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue }
import pekko.stream.{ Attributes, Outlet, SourceShape }
import javax.jms

import scala.concurrent.{ ExecutionContext, TimeoutException }
import scala.util.{ Failure, Success }

/**
 * Internal API.
 */
@InternalApi
private[jms] final class JmsTxSourceStage(settings: JmsConsumerSettings, destination: Destination)
    extends GraphStageWithMaterializedValue[SourceShape[TxEnvelope], JmsConsumerMatValue] {

  private val out = Outlet[TxEnvelope]("JmsSource.out")

  override def shape: SourceShape[TxEnvelope] = SourceShape[TxEnvelope](out)

  override protected def initialAttributes: Attributes = Attributes.name("JmsTxConsumer")

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, JmsConsumerMatValue) = {
    val logic = new JmsTxSourceStageLogic(inheritedAttributes)
    (logic, logic.consumerControl)
  }

  private final class JmsTxSourceStageLogic(inheritedAttributes: Attributes)
      extends SourceStageLogic[TxEnvelope](shape, out, settings, destination, inheritedAttributes) {

    // Queue of pending commit/rollback actions awaiting execution on the JMS provider's thread.
    // Actions are enqueued when the user calls commit()/rollback() on a TxEnvelope, and drained
    // at the start of the next onMessage() callback so they run on the correct thread.
    // This avoids blocking the JMS provider's delivery thread while waiting for user acknowledgment,
    // while still satisfying the JMS spec requirement that commit/rollback execute on the
    // session's delivery thread (enforced strictly by IBM MQ).
    private val pendingActions = new ConcurrentLinkedQueue[() => Unit]()

    protected def createSession(connection: jms.Connection, createDestination: jms.Session => javax.jms.Destination) = {
      val session =
        connection.createSession(true, settings.acknowledgeMode.getOrElse(AcknowledgeMode.SessionTransacted).mode)
      new JmsConsumerSession(connection, session, createDestination(session), graphStageDestination)
    }

    protected def pushMessage(msg: TxEnvelope): Unit = push(out, msg)

    override protected def onSessionOpened(jmsSession: JmsConsumerSession): Unit =
      jmsSession match {
        case session: JmsSession =>
          session
            .createConsumer(settings.selector)
            .map { consumer =>
              consumer.setMessageListener(new jms.MessageListener {

                def onMessage(message: jms.Message): Unit =
                  try {
                    // Drain any pending commit/rollback actions from previous messages.
                    // This runs on the JMS provider's delivery thread, satisfying the JMS spec
                    // requirement that commit/rollback must happen on the session's thread.
                    drainPendingActions()

                    val envelope = TxEnvelope(message, session)
                    handleMessage.invoke(envelope)
                    // Don't block here — listen for the user's acknowledgment and enqueue it
                    // for execution on the next onMessage() callback (on the provider's thread).
                    envelope.commitFuture.onComplete {
                      case Success(action) => pendingActions.add(action)
                      case Failure(_)      => // stage already failed or timed out
                    }(ExecutionContext.parasitic)
                  } catch {
                    case e: IllegalArgumentException => handleError.invoke(e) // Invalid envelope. Fail the stage.
                    case e: jms.JMSException         => handleError.invoke(e)
                  }
              })
            }
            .onComplete(sessionOpenedCB.invoke)

        case _ =>
          throw new IllegalArgumentException(
            "Session must be of type JmsSession, it is a " +
            jmsSession.getClass.getName)
      }

    /**
     * Execute any pending commit/rollback actions that were enqueued by user acknowledgment.
     * Must be called on the JMS provider's delivery thread.
     */
    private def drainPendingActions(): Unit = {
      var action = pendingActions.poll()
      while (action != null) {
        try {
          action()
        } catch {
          case e: jms.JMSException =>
            log.error(e, "Failed to execute pending JMS action")
        }
        action = pendingActions.poll()
      }
    }
  }

}
