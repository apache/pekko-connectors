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

package akka.stream.alpakka.ironmq

import scala.concurrent.duration.{ Duration, FiniteDuration }
import akka.util.JavaDurationConverters._

case class PushMessage(body: String, delay: FiniteDuration = Duration.Zero)

object PushMessage {

  def create(body: String): PushMessage = PushMessage(body)

  def create(body: String, duration: java.time.Duration): PushMessage =
    PushMessage(body, duration.asScala)
}

/**
 * The message consumed from IronMq.
 *
 * @param messageId The unique id of the message.
 * @param body The pushed message content.
 * @param noOfReservations It is the count of how many time the message has been reserved (and released or expired) previously
 */
case class Message(messageId: Message.Id, body: String, noOfReservations: Int)

object Message {

  case class Id(value: String) extends AnyVal {
    override def toString: String = value
  }

  case class Ids(ids: List[Id])
}
