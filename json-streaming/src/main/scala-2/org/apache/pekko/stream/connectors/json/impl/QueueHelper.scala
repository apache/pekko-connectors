/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.stream.connectors.json.impl

import org.apache.pekko.util.ByteString

import scala.collection.immutable.Queue

private[impl] object QueueHelper {
  @inline final def enqueue(queue: Queue[ByteString], byteString: ByteString): Queue[ByteString] =
    queue.enqueue(byteString)
}
