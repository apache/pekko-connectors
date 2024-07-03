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
import pekko.stream.connectors.jakartams.{ Destination, JmsBrowseSettings }
import pekko.stream.stage.{ GraphStage, GraphStageLogic, OutHandler }
import pekko.stream.{ ActorAttributes, Attributes, Outlet, SourceShape }

/**
 * Internal API.
 */
@InternalApi
private[jakartams] final class JmsBrowseStage(settings: JmsBrowseSettings, queue: Destination)
    extends GraphStage[SourceShape[jms.Message]] {
  private val out = Outlet[jms.Message]("JmsBrowseStage.out")
  val shape = SourceShape(out)

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name("JmsBrowse") and ActorAttributes.IODispatcher

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {
      setHandler(out, this)

      var connection: jms.Connection = _
      var session: jms.Session = _
      var browser: jms.QueueBrowser = _
      var messages: java.util.Enumeration[jms.Message] = _

      override def preStart(): Unit = {
        val ackMode = settings.acknowledgeMode.mode
        connection = settings.connectionFactory.createConnection()
        connection.start()

        session = connection.createSession(false, ackMode)
        browser = session.createBrowser(session.createQueue(queue.name), settings.selector.orNull)
        messages = browser.getEnumeration.asInstanceOf[java.util.Enumeration[jms.Message]]
      }

      override def postStop(): Unit = {
        messages = null
        if (browser ne null) {
          browser.close()
          browser = null
        }
        if (session ne null) {
          session.close()
          session = null
        }
        if (connection ne null) {
          connection.close()
          connection = null
        }
      }

      def onPull(): Unit =
        if (messages.hasMoreElements) {
          push(out, messages.nextElement())
        } else {
          complete(out)
        }
    }
}
