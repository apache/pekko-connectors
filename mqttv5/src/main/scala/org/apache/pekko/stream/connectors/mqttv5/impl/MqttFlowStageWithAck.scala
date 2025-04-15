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

package org.apache.pekko.stream.connectors.mqttv5.impl

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise

import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream._
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttMessageWithAck
import org.apache.pekko.stream.stage._
import org.eclipse.paho.mqttv5.client.IMqttToken

/**
 * INTERNAL API
 */

@InternalApi
private[mqttv5] final class MqttFlowStageWithAck(connectionSettings: MqttConnectionSettings,
    subscriptions: Map[String, MqttQoS],
    bufferSize: Int,
    defaultQoS: MqttQoS,
    manualAcks: Boolean = false)
    extends GraphStageWithMaterializedValue[FlowShape[MqttMessageWithAck, MqttMessageWithAck], Future[Done]] {

  private val in = Inlet[MqttMessageWithAck]("MqttFlow.in")
  private val out = Outlet[MqttMessageWithAck]("MqttFlow.out")
  override val shape: Shape = FlowShape(in, out)

  override protected def initialAttributes: Attributes = Attributes.name("MqttFlow")

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val subscriptionPromise = Promise[Done]()

    val logic = new MqttFlowWithAckStageLogic(
      in = in,
      out = out,
      shape = shape,
      subscriptionPromise = subscriptionPromise,
      connectionSettings = connectionSettings,
      subscriptions = subscriptions,
      bufferSize = bufferSize,
      defaultQoS = defaultQoS,
      manualAcks = manualAcks
    )
    (logic, subscriptionPromise.future)
  }

}

class MqttFlowWithAckStageLogic(
    in: Inlet[MqttMessageWithAck],
    out: Outlet[MqttMessageWithAck],
    shape: Shape,
    subscriptionPromise: Promise[Done],
    connectionSettings: MqttConnectionSettings,
    subscriptions: Map[String, MqttQoS],
    bufferSize: Int,
    defaultQoS: MqttQoS,
    manualAcks: Boolean
) extends MqttFlowStageLogic[MqttMessageWithAck](
      in = in,
      out = out,
      shape = shape,
      subscriptionPromise = subscriptionPromise,
      connectionSettings = connectionSettings,
      subscriptions = subscriptions,
      bufferSize = bufferSize,
      defaultQoS = defaultQoS,
      manualAcks = manualAcks
    ) {

  private val messagesToAck: mutable.HashMap[Int, MqttMessageWithAck] = mutable.HashMap()

  override def handleDeliveryComplete(token: IMqttToken): Unit = {
    if (messagesToAck.isDefinedAt(token.getMessageId)) {
      messagesToAck(token.getMessageId).ack()
      messagesToAck.remove(token.getMessageId)
    }
  }

  override def publishPending(msg: MqttMessageWithAck): Unit = {
    val publish = publishToMqtt(msg.message)
    messagesToAck ++= mutable.HashMap(publish.getMessageId -> msg)
  }

}
