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

package akka.stream.alpakka.azure.storagequeue.javadsl

import com.microsoft.azure.storage.queue.{ CloudQueue, CloudQueueMessage }
import akka.stream.alpakka.azure.storagequeue.AzureQueueSourceSettings
import akka.stream.javadsl.Source
import akka.NotUsed
import java.util.function.Supplier

import akka.stream.alpakka.azure.storagequeue.impl.AzureQueueSourceStage

object AzureQueueSource {

  /**
   * Java API: creates a [[AzureQueueSource]] for a Azure CloudQueue.
   */
  def create(cloudQueue: Supplier[CloudQueue], settings: AzureQueueSourceSettings): Source[CloudQueueMessage, NotUsed] =
    Source.fromGraph(new AzureQueueSourceStage(() => cloudQueue.get(), settings))

  def create(cloudQueue: Supplier[CloudQueue]): Source[CloudQueueMessage, NotUsed] =
    create(cloudQueue, AzureQueueSourceSettings())
}
