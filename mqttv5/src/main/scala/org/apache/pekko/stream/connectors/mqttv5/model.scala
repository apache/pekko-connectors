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

package org.apache.pekko.stream.connectors.mqttv5

import org.apache.pekko
import pekko.util.ccompat.JavaConverters._

import scala.collection.immutable

final class MqttUserProperty private (val key: String, val value: String) {
  override def toString = s"MqttUserProperty(key=$key,value=$value)"

  override def equals(other: Any): Boolean = other match {
    case that: MqttUserProperty =>
      java.util.Objects.equals(this.key, that.key) &&
      java.util.Objects.equals(this.value, that.value)
    case _ => false
  }

  override def hashCode(): Int = java.util.Objects.hash(key, value)
}

object MqttUserProperty {

  /** Scala API */
  def apply(key: String, value: String): MqttUserProperty = new MqttUserProperty(key, value)

  /** Java API */
  def create(key: String, value: String): MqttUserProperty = new MqttUserProperty(key, value)
}

final class MqttMessage private (
    val topic: String,
    val payload: org.apache.pekko.util.ByteString,
    val qos: Option[MqttQoS],
    val retained: Boolean,
    val userProperties: Array[MqttUserProperty]
) {

  def withTopic(value: String): MqttMessage = copy(topic = value)
  def withPayload(value: pekko.util.ByteString): MqttMessage = copy(payload = value)
  def withPayload(value: Array[Byte]): MqttMessage = copy(payload = pekko.util.ByteString(value))
  def withQos(value: MqttQoS): MqttMessage = copy(qos = Option(value))
  def withRetained(value: Boolean): MqttMessage = if (retained == value) this else copy(retained = value)

  /** Scala API */
  def withUserProperties(value: immutable.Seq[MqttUserProperty]): MqttMessage =
    copy(userProperties = value.toArray)

  /** Java API */
  def withUserProperties(value: java.util.List[MqttUserProperty]): MqttMessage =
    copy(userProperties = value.asScala.toArray)

  private def copy(
      topic: String = topic,
      payload: pekko.util.ByteString = payload,
      qos: Option[MqttQoS] = qos,
      retained: Boolean = retained,
      userProperties: Array[MqttUserProperty] = userProperties): MqttMessage =
    new MqttMessage(topic = topic, payload = payload, qos = qos, retained = retained, userProperties = userProperties)

  override def toString =
    s"""MqttMessage(topic=$topic,payload=$payload,qos=$qos,retained=$retained,userProperties=${userProperties.mkString(
        "[", ", ", "]")})"""

  override def equals(other: Any): Boolean = other match {
    case that: MqttMessage =>
      java.util.Objects.equals(this.topic, that.topic) &&
      java.util.Objects.equals(this.payload, that.payload) &&
      java.util.Objects.equals(this.qos, that.qos) &&
      java.util.Objects.equals(this.retained, that.retained) &&
      java.util.Objects.equals(this.userProperties.toSeq, that.userProperties.toSeq)
    case _ => false
  }

  override def hashCode(): Int =
    java.util.Objects.hash(topic, payload, qos, Boolean.box(retained), userProperties.toSeq)
}

object MqttMessage {

  /** Scala API */
  def apply(
      topic: String,
      payload: pekko.util.ByteString): MqttMessage = new MqttMessage(
    topic,
    payload,
    qos = None,
    retained = false,
    userProperties = Array.empty)

  /** Java API */
  def create(
      topic: String,
      payload: pekko.util.ByteString): MqttMessage = new MqttMessage(
    topic,
    payload,
    qos = None,
    retained = false,
    userProperties = Array.empty)
}
