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

package org.apache.pekko.stream.connectors.azure.storagequeue.javadsl

import com.microsoft.azure.storage.queue.{ CloudQueue, CloudQueueMessage }
import org.apache.pekko
import pekko.stream.connectors.azure.storagequeue.impl.AzureQueueSinkFunctions
import pekko.stream.javadsl.Sink
import pekko.Done
import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import pekko.stream.connectors.azure.storagequeue.DeleteOrUpdateMessage

object AzureQueueSink {

  /**
   * JavaAPI: creates a [[pekko.stream.javadsl.Sink]] which queues message to an Azure Storage Queue.
   */
  def create(cloudQueue: Supplier[CloudQueue]): Sink[CloudQueueMessage, CompletionStage[Done]] =
    fromFunction(AzureQueueSinkFunctions.addMessage(() => cloudQueue.get)(_))

  /**
   * Internal API
   */
  private[javadsl] def fromFunction[T](f: T => Unit): Sink[T, CompletionStage[Done]] = {
    import pekko.stream.connectors.azure.storagequeue.scaladsl.{ AzureQueueSink => AzureQueueSinkScalaDSL }
    import pekko.util.FutureConverters._
    AzureQueueSinkScalaDSL.fromFunction(f).mapMaterializedValue(_.asJava).asJava
  }
}

class MessageWithTimeouts(val message: CloudQueueMessage, val timeToLive: Int, val initialVisibility: Int)

object AzureQueueWithTimeoutsSink {

  /**
   * JavaAPI: creates an [[pekko.stream.javadsl.Sink]] with queues message to an Azure Storage Queue.
   * This is the same as [[AzureQueueSink.create]] expect that it takes instead
   * of a [[com.microsoft.azure.storage.queue.CloudQueueMessage]] a [[MessageWithTimeouts]].
   */
  def create(cloudQueue: Supplier[CloudQueue]): Sink[MessageWithTimeouts, CompletionStage[Done]] =
    AzureQueueSink.fromFunction[MessageWithTimeouts] { input =>
      AzureQueueSinkFunctions
        .addMessage(() => cloudQueue.get)(input.message, input.timeToLive, input.initialVisibility)
    }
}

object AzureQueueDeleteSink {

  /**
   * JavaAPI: creates a [[pekko.stream.javadsl.Sink]] which deletes messages from an Azure Storage Queue.
   */
  def create(cloudQueue: Supplier[CloudQueue]): Sink[CloudQueueMessage, CompletionStage[Done]] =
    AzureQueueSink.fromFunction[CloudQueueMessage](AzureQueueSinkFunctions.deleteMessage(() => cloudQueue.get)(_))
}

class MessageAndDeleteOrUpdate(val message: CloudQueueMessage, val op: DeleteOrUpdateMessage)

object AzureQueueDeleteOrUpdateSink {

  /**
   * JavaAPI: creates a [[pekko.stream.javadsl.Sink]] which deletes or updates the visibility timeout of messages
   * in an Azure Storage Queue.
   */
  def create(cloudQueue: Supplier[CloudQueue]): Sink[MessageAndDeleteOrUpdate, CompletionStage[Done]] =
    AzureQueueSink.fromFunction[MessageAndDeleteOrUpdate](input =>
      AzureQueueSinkFunctions.deleteOrUpdateMessage(() => cloudQueue.get)(input.message, input.op))
}
