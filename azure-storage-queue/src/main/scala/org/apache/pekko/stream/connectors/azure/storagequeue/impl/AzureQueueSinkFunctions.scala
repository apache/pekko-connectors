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

package org.apache.pekko.stream.connectors.azure.storagequeue.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.azure.storagequeue.DeleteOrUpdateMessage
import pekko.stream.connectors.azure.storagequeue.DeleteOrUpdateMessage.{ Delete, UpdateVisibility }
import com.microsoft.azure.storage.queue.{ CloudQueue, CloudQueueMessage }

/**
 * INTERNAL API
 */
@InternalApi private[storagequeue] object AzureQueueSinkFunctions {
  def addMessage(
      cloudQueue: () => CloudQueue)(
      msg: CloudQueueMessage, timeToLive: Int = 0, initialVisibilityTimeout: Int = 0): Unit =
    cloudQueue().addMessage(msg, timeToLive, initialVisibilityTimeout, null, null)

  def deleteMessage(
      cloudQueue: () => CloudQueue)(msg: CloudQueueMessage): Unit =
    cloudQueue().deleteMessage(msg)

  def updateMessage(cloudQueue: () => CloudQueue)(msg: CloudQueueMessage, timeout: Int): Unit =
    cloudQueue().updateMessage(msg, timeout)

  def deleteOrUpdateMessage(
      cloudQueue: () => CloudQueue)(msg: CloudQueueMessage, op: DeleteOrUpdateMessage): Unit =
    op match {
      case _: Delete           => deleteMessage(cloudQueue)(msg)
      case m: UpdateVisibility => updateMessage(cloudQueue)(msg, m.timeout)
    }
}
