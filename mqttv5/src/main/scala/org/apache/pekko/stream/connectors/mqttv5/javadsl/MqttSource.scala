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
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions
import org.apache.pekko.stream.connectors.mqttv5.scaladsl
import org.apache.pekko.stream.javadsl.Source
import org.apache.scala.jdk.FutureConverters._

/**
 * Java API
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
      bufferSize: Int): Source[MqttMessage, CompletionStage[Done]] =
    scaladsl.MqttSource
      .atMostOnce(settings, subscriptions, bufferSize)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Create a source subscribing to MQTT messages with a commit handle to acknowledge message reception.
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   */
  def atLeastOnce(settings: MqttConnectionSettings,
      subscriptions: MqttSubscriptions,
      bufferSize: Int): Source[MqttMessageWithAck, CompletionStage[Done]] =
    scaladsl.MqttSource
      .atLeastOnce(settings, subscriptions, bufferSize)
      .map(MqttMessageWithAck.toJava)
      .mapMaterializedValue(_.asJava)
      .asJava
}
