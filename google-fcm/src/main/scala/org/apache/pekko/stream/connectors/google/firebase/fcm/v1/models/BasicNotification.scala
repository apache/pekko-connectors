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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.models

/**
 * Notification model.
 * @see https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages#Notification
 */
case class BasicNotification(title: String, body: String, image: Option[String] = None) {
  def withTitle(value: String): BasicNotification = this.copy(title = value)
  def withBody(value: String): BasicNotification = this.copy(body = value)
  def withImage(value: String): BasicNotification = this.copy(image = Option(value))
}

object BasicNotification {
  def create(title: String, body: String): BasicNotification =
    BasicNotification(title = title, body = body, image = None)
  def create(title: String, body: String, image: String): BasicNotification =
    BasicNotification(title = title, body = body, image = Option(image))
}
