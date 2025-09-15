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

package org.apache.pekko.stream.connectors.mqttv5.scaladsl

import scala.concurrent.Future

import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.javadsl
import org.apache.scala.jdk.FutureConverters._

/**
 * Scala API
 *
 * MQTT Message and a handle to acknowledge message reception to MQTT.
 */
trait MqttMessageWithAck {

  /**
   * The message received from MQTT.
   */
  val message: MqttMessage

  /**
   * Signals `messageArrivedComplete` to MQTT.
   *
   * @return a future indicating, if the acknowledge reached MQTT
   */
  def ack(): Future[Done]
}
/*
 * INTERNAL API
 */
@InternalApi
private[scaladsl] object MqttMessageWithAck {
  def fromJava(e: javadsl.MqttMessageWithAck): MqttMessageWithAck =
    new MqttMessageWithAck {
      override val message: MqttMessage = e.message

      /**
       * Signals `messageArrivedComplete` to MQTT.
       *
       * @return a future indicating, if the acknowledge reached MQTT
       */
      override def ack(): Future[Done] = e.ack().asScala
    }
}
