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
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Source

/**
 * Scala API
 *
 * MQTT source factory.
 */
object MqttSource {

  /**
   * Create a source subscribing to MQTT messages (without a commit handle).
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   */
  def atMostOnce(settings: MqttConnectionSettings,
      subscriptions: MqttSubscriptions,
      bufferSize: Int): Source[MqttMessage, Future[Done]] =
    Source.maybe
      .viaMat(
        MqttFlow.atMostOnce(settings, subscriptions, bufferSize, defaultQos = MqttQoS.AtLeastOnce))(Keep.right)

  /**
   * Create a source subscribing to MQTT messages with a commit handle to acknowledge message reception.
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   */
  def atLeastOnce(settings: MqttConnectionSettings,
      subscriptions: MqttSubscriptions,
      bufferSize: Int): Source[MqttMessageWithAck, Future[Done]] =
    Source.maybe.viaMat(
      MqttFlow.atLeastOnce(settings, subscriptions, bufferSize, defaultQos = MqttQoS.AtLeastOnce))(Keep.right)

}
