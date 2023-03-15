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

package org.apache.pekko.stream.connectors.mqtt.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.mqtt._
import pekko.stream.javadsl.Flow

import scala.compat.java8.FutureConverters._

/**
 * Java API
 *
 * MQTT flow factory.
 */
object MqttFlow {

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages (without a commit handle).
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  def atMostOnce(settings: MqttConnectionSettings,
      subscriptions: MqttSubscriptions,
      bufferSize: Int,
      defaultQos: MqttQoS): Flow[MqttMessage, MqttMessage, CompletionStage[Done]] =
    scaladsl.MqttFlow
      .atMostOnce(settings, subscriptions, bufferSize, defaultQos)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages with a commit handle to acknowledge message reception.
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  def atLeastOnce(
      settings: MqttConnectionSettings,
      subscriptions: MqttSubscriptions,
      bufferSize: Int,
      defaultQos: MqttQoS): Flow[MqttMessage, MqttMessageWithAck, CompletionStage[Done]] =
    scaladsl.MqttFlow
      .atLeastOnce(settings, subscriptions, bufferSize, defaultQos)
      .map(MqttMessageWithAck.toJava)
      .mapMaterializedValue(_.toJava)
      .asJava

  /**
   * Create a flow to send messages to MQTT , send acknowledge AND subscribe to MQTT messages with a commit handle to acknowledge message reception.
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  def atLeastOnceWithAck(
      settings: MqttConnectionSettings,
      subscriptions: MqttSubscriptions,
      bufferSize: Int,
      defaultQos: MqttQoS): Flow[MqttMessageWithAck, MqttMessageWithAck, CompletionStage[Done]] =
    scaladsl.MqttFlow
      .atLeastOnceWithAckForJava(settings, subscriptions, bufferSize, defaultQos)
      .map(MqttMessageWithAck.toJava)
      .mapMaterializedValue(_.toJava)
      .asJava
}
