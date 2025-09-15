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

package org.apache.pekko.stream.connectors.mqttv5.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.scaladsl

import scala.jdk.FutureConverters._

/**
 * Java API
 *
 * MQTT Message and a handle to acknowledge message reception to MQTT.
 */
sealed trait MqttMessageWithAck {

  /**
   * The message received from MQTT.
   */
  val message: MqttMessage

  /**
   * Signals `messageArrivedComplete` to MQTT.
   *
   * @return completion indicating, if the acknowledge reached MQTT
   */
  def ack(): CompletionStage[Done]
}

/**
 * INTERNAL API
 */
@InternalApi
private[javadsl] object MqttMessageWithAck {
  def toJava(cm: scaladsl.MqttMessageWithAck): MqttMessageWithAck = new MqttMessageWithAck {
    override val message: MqttMessage = cm.message
    override def ack(): CompletionStage[Done] = cm.ack().asJava
  }
}

abstract class MqttMessageWithAckImpl extends MqttMessageWithAck
