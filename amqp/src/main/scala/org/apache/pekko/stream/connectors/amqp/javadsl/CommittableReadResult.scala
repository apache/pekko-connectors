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

package org.apache.pekko.stream.connectors.amqp.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.amqp.ReadResult
import org.apache.pekko.stream.connectors.amqp.scaladsl

import scala.compat.java8.FutureConverters._

final class CommittableReadResult(cm: scaladsl.CommittableReadResult) {
  val message: ReadResult = cm.message

  def ack(): CompletionStage[Done] = ack(false)
  def ack(multiple: Boolean): CompletionStage[Done] = cm.ack(multiple).toJava

  def nack(): CompletionStage[Done] = nack(false, true)
  def nack(multiple: Boolean, requeue: Boolean): CompletionStage[Done] =
    cm.nack(multiple, requeue).toJava
}
