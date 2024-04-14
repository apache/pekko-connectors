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

package org.apache.pekko.stream.connectors.ironmq.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.ironmq.Message

/**
 * Internal API. The message reserved from IronMq.
 *
 * This message has been ask to be reserved from IronMq. It contains both the message itself and the reservation id.
 *
 * @param reservationId The reservation id needed to release or delete the message.
 * @param message The fetched message.
 */
@InternalApi
private[ironmq] final case class ReservedMessage(reservationId: Reservation.Id, message: Message) {
  val messageId: Message.Id = message.messageId
  val messageBody: String = message.body
  val reservation: Reservation = Reservation(messageId, reservationId)
}

/**
 * Internal API.
 *
 * Represent a message reservation. It is used when you need to delete or release a reserved message. It is obtained from
 * a [[ReservedMessage]] by message id and reservation id.
 *
 * @param messageId The previously reserved message Id.
 * @param reservationId The reservation id
 */
@InternalApi
private[ironmq] final case class Reservation(messageId: Message.Id, reservationId: Reservation.Id)

/**
 * Internal API.
 */
@InternalApi
private[ironmq] object Reservation {
  final case class Id(value: String) extends AnyVal {
    override def toString: String = value
  }
}
