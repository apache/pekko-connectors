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

package org.apache.pekko.stream.connectors.azure.storagequeue.javadsl

import com.microsoft.azure.storage.queue.{ CloudQueue, CloudQueueMessage }
import org.apache.pekko
import pekko.stream.connectors.azure.storagequeue.AzureQueueSourceSettings
import pekko.stream.javadsl.Source
import pekko.NotUsed
import java.util.function.Supplier

import pekko.stream.connectors.azure.storagequeue.impl.AzureQueueSourceStage

object AzureQueueSource {

  /**
   * Java API: creates a [[AzureQueueSource]] for a Azure CloudQueue.
   */
  def create(cloudQueue: Supplier[CloudQueue], settings: AzureQueueSourceSettings): Source[CloudQueueMessage, NotUsed] =
    Source.fromGraph(new AzureQueueSourceStage(() => cloudQueue.get(), settings))

  def create(cloudQueue: Supplier[CloudQueue]): Source[CloudQueueMessage, NotUsed] =
    create(cloudQueue, AzureQueueSourceSettings())
}
