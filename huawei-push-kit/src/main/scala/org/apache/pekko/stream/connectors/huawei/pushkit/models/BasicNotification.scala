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

package org.apache.pekko.stream.connectors.huawei.pushkit.models

/**
 * Notification model.
 * @see https://developer.huawei.com/consumer/en/doc/development/HMSCore-References-V5/https-send-api-0000001050986197-V5#EN-US_TOPIC_0000001134031085
 */
final case class BasicNotification(title: Option[String] = None, body: Option[String] = None,
    image: Option[String] = None) {
  def withTitle(value: String): BasicNotification = this.copy(title = Option(value))
  def withBody(value: String): BasicNotification = this.copy(body = Option(value))
  def withImage(value: String): BasicNotification = this.copy(image = Option(value))
}

object BasicNotification {
  val empty: BasicNotification = BasicNotification()
  def fromJava(): BasicNotification = empty
}
