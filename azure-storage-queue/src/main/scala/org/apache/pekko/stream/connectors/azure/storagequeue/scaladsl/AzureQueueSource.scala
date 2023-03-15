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

package org.apache.pekko.stream.connectors.azure.storagequeue.scaladsl

import com.microsoft.azure.storage.queue.{ CloudQueue, CloudQueueMessage }
import org.apache.pekko
import pekko.stream.connectors.azure.storagequeue.AzureQueueSourceSettings
import pekko.stream.scaladsl.Source
import pekko.NotUsed
import pekko.stream.connectors.azure.storagequeue.impl.AzureQueueSourceStage

object AzureQueueSource {

  /**
   * Scala API: creates a [[AzureQueueSource]] for a Azure CloudQueue.
   */
  def apply(
      cloudQueue: () => CloudQueue,
      settings: AzureQueueSourceSettings = AzureQueueSourceSettings()): Source[CloudQueueMessage, NotUsed] =
    Source.fromGraph(new AzureQueueSourceStage(cloudQueue, settings))
}
