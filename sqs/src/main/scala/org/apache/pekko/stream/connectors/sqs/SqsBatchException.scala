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

package org.apache.pekko.stream.connectors.sqs

import org.apache.pekko.annotation.InternalApi

final class SqsBatchException @InternalApi private[sqs] (val batchSize: Int, message: String)
    extends Exception(message) {

  @InternalApi
  private[sqs] def this(batchSize: Int, cause: Throwable) = {
    this(batchSize, cause.getMessage)
    initCause(cause)
  }

  @InternalApi
  private[sqs] def this(batchSize: Int, message: String, cause: Throwable) = {
    this(batchSize, message)
    initCause(cause)
  }

  /** Java API */
  def getBatchSize: Int = batchSize
}
