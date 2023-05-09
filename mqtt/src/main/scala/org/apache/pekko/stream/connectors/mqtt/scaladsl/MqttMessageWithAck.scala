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

package org.apache.pekko.stream.connectors.mqtt.scaladsl

import org.apache.pekko
import pekko.Done
import pekko.annotation.InternalApi
import pekko.stream.connectors.mqtt.MqttMessage
import pekko.util.FutureConverters._

import scala.concurrent.Future

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
  def fromJava(e: pekko.stream.connectors.mqtt.javadsl.MqttMessageWithAck): MqttMessageWithAck =
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
