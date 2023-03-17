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

package org.apache.pekko.stream.connectors.amqp.impl

import org.apache.pekko
import pekko.stream.connectors.amqp.{ AmqpConnectorSettings, BindingDeclaration, ExchangeDeclaration, QueueDeclaration }
import pekko.stream.stage.{ AsyncCallback, GraphStageLogic }
import com.rabbitmq.client._

import scala.util.control.NonFatal

private trait AmqpConnectorLogic { this: GraphStageLogic =>

  private var connection: Connection = _
  protected var channel: Channel = _

  protected lazy val shutdownCallback: AsyncCallback[Throwable] = getAsyncCallback(onFailure)
  private lazy val shutdownListener = new ShutdownListener {
    override def shutdownCompleted(cause: ShutdownSignalException): Unit = shutdownCallback.invoke(cause)
  }

  def settings: AmqpConnectorSettings
  def whenConnected(): Unit
  def onFailure(ex: Throwable): Unit = failStage(ex)

  final override def preStart(): Unit =
    try {
      connection = settings.connectionProvider.get
      channel = connection.createChannel

      connection.addShutdownListener(shutdownListener)
      channel.addShutdownListener(shutdownListener)

      import org.apache.pekko.util.ccompat.JavaConverters._

      settings.declarations.foreach {
        case d: QueueDeclaration =>
          channel.queueDeclare(
            d.name,
            d.durable,
            d.exclusive,
            d.autoDelete,
            d.arguments.asJava)

        case d: BindingDeclaration =>
          channel.queueBind(
            d.queue,
            d.exchange,
            d.routingKey.getOrElse(""),
            d.arguments.asJava)

        case d: ExchangeDeclaration =>
          channel.exchangeDeclare(
            d.name,
            d.exchangeType,
            d.durable,
            d.autoDelete,
            d.internal,
            d.arguments.asJava)
      }

      whenConnected()
    } catch {
      case NonFatal(e) => onFailure(e)
    }

  /** remember to call if overriding! */
  override def postStop(): Unit = {
    if ((channel ne null) && channel.isOpen) channel.close()
    channel = null

    if (connection ne null) {
      connection.removeShutdownListener(shutdownListener)
      settings.connectionProvider.release(connection)
      connection = null
    }
  }
}
