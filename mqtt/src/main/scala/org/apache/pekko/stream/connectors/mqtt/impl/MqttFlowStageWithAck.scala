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

package org.apache.pekko.stream.connectors.mqtt.impl

import org.apache.pekko
import pekko.Done
import pekko.annotation.InternalApi
import pekko.stream._
import pekko.stream.connectors.mqtt._
import pekko.stream.connectors.mqtt.scaladsl.MqttMessageWithAck
import pekko.stream.stage._
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken

import scala.collection.mutable
import scala.concurrent.{ Future, Promise }

/**
 * INTERNAL API
 */

@InternalApi
private[mqtt] final class MqttFlowStageWithAck(connectionSettings: MqttConnectionSettings,
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

    val logic = new MqttFlowWithAckStageLogic(in,
      out,
      shape,
      subscriptionPromise,
      connectionSettings,
      subscriptions,
      bufferSize,
      defaultQoS,
      manualAcks)
    (logic, subscriptionPromise.future)
  }

}

class MqttFlowWithAckStageLogic(in: Inlet[MqttMessageWithAck],
    out: Outlet[MqttMessageWithAck],
    shape: Shape,
    subscriptionPromise: Promise[Done],
    connectionSettings: MqttConnectionSettings,
    subscriptions: Map[String, MqttQoS],
    bufferSize: Int,
    defaultQoS: MqttQoS,
    manualAcks: Boolean)
    extends MqttFlowStageLogic[MqttMessageWithAck](in,
      out,
      shape,
      subscriptionPromise,
      connectionSettings,
      subscriptions,
      bufferSize,
      defaultQoS,
      manualAcks) {

  private val messagesToAck: mutable.HashMap[Int, MqttMessageWithAck] = mutable.HashMap()

  override def handleDeliveryComplete(token: IMqttDeliveryToken): Unit =
    if (messagesToAck.isDefinedAt(token.getMessageId)) {
      messagesToAck(token.getMessageId).ack()
      messagesToAck.remove(token.getMessageId)
    }

  override def publishPending(msg: MqttMessageWithAck): Unit = {
    val publish = publishToMqtt(msg.message)
    messagesToAck ++= mutable.HashMap(publish.getMessageId -> msg)
  }

}
