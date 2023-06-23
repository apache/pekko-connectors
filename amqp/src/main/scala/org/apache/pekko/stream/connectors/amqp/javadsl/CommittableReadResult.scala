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

package org.apache.pekko.stream.connectors.amqp.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.amqp.ReadResult
import pekko.stream.connectors.amqp.scaladsl
import pekko.util.FutureConverters._

final class CommittableReadResult(cm: scaladsl.CommittableReadResult) {
  val message: ReadResult = cm.message

  def ack(): CompletionStage[Done] = ack(false)
  def ack(multiple: Boolean): CompletionStage[Done] = cm.ack(multiple).asJava

  def nack(): CompletionStage[Done] = nack(false, true)
  def nack(multiple: Boolean, requeue: Boolean): CompletionStage[Done] =
    cm.nack(multiple, requeue).asJava
}
