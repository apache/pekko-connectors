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

package org.apache.pekko.stream.connectors.ironmq.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.stream.connectors.ironmq.Message

/**
 * Commit an offset that is included in a [[CommittableMessage]].
 */
trait Committable {
  def commit(): CompletionStage[Done]
}

/**
 * A [[Committable]] wrapper around the IronMq [[Message]].
 */
trait CommittableMessage extends Committable {
  def message: Message
}
