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

package akka.stream.alpakka.amqp.scaladsl

import akka.Done
import akka.stream.alpakka.amqp.ReadResult

import scala.concurrent.Future

trait CommittableReadResult {
  val message: ReadResult
  def ack(multiple: Boolean = false): Future[Done]
  def nack(multiple: Boolean = false, requeue: Boolean = true): Future[Done]
}
