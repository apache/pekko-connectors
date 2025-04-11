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
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.MqttSubscriptions
import org.apache.pekko.stream.javadsl.Keep
import org.apache.pekko.stream.javadsl.Sink

/**
 * Java API
 *
 * MQTT sink factory.
 */
object MqttSink {

  /**
   * Create a sink sending messages to MQTT.
   *
   * The materialized value completes on stream completion.
   *
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  def create(connectionSettings: MqttConnectionSettings,
      defaultQos: MqttQoS): Sink[MqttMessage, CompletionStage[Done]] =
    MqttFlow
      .atMostOnce(connectionSettings, MqttSubscriptions.empty, bufferSize = 0, defaultQos)
      .toMat(Sink.ignore[MqttMessage](), Keep.right[CompletionStage[Done], CompletionStage[Done]])
}
