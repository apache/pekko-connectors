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
import org.apache.pekko.stream.connectors.azure.storagequeue.impl.AzureQueueSinkFunctions
import org.apache.pekko.stream.scaladsl.{ Flow, Keep, Sink }
import org.apache.pekko.Done

import scala.concurrent.Future
import org.apache.pekko.stream.impl.Stages.DefaultAttributes.IODispatcher
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.connectors.azure.storagequeue.DeleteOrUpdateMessage

object AzureQueueSink {

  /**
   * ScalaAPI: creates a [[org.apache.pekko.stream.scaladsl.Sink]] which queues message to an Azure Storage Queue.
   */
  def apply(cloudQueue: () => CloudQueue): Sink[CloudQueueMessage, Future[Done]] =
    fromFunction(AzureQueueSinkFunctions.addMessage(cloudQueue)(_))

  /**
   * Internal API
   */
  def fromFunction[T](f: T => Unit): Sink[T, Future[Done]] =
    Flow
      .fromFunction(f)
      .addAttributes(Attributes(IODispatcher))
      .toMat(Sink.ignore)(Keep.right)
}

object AzureQueueWithTimeoutsSink {

  /**
   * ScalaAPI: creates an [[org.apache.pekko.stream.scaladsl.Sink]] with queues message to an Azure Storage Queue.
   * This is the same as [[AzureQueueSink.apply]] expect that the sink takes instead
   * of a [[com.microsoft.azure.storage.queue.CouldQueueMessage]] a tuple
   * with (CouldQueueMessage, timeToLive, initialVisibilityTimeout).
   */
  def apply(
      cloudQueue: () => CloudQueue): Sink[(CloudQueueMessage, Int, Int), Future[Done]] =
    AzureQueueSink.fromFunction(tup => AzureQueueSinkFunctions.addMessage(cloudQueue)(tup._1, tup._2, tup._3))
}

object AzureQueueDeleteSink {

  /**
   * ScalaAPI: creates a [[org.apache.pekko.stream.scaladsl.Sink]] which deletes messages from an Azure Storage Queue.
   */
  def apply(cloudQueue: () => CloudQueue): Sink[CloudQueueMessage, Future[Done]] =
    AzureQueueSink.fromFunction(AzureQueueSinkFunctions.deleteMessage(cloudQueue)(_))
}

object AzureQueueDeleteOrUpdateSink {

  /**
   * ScalaAPI: creates a [[org.apache.pekko.stream.scaladsl.Sink]] which deletes or updates the visibility timeout of messages
   * in an Azure Storage Queue.
   */
  def apply(
      cloudQueue: () => CloudQueue): Sink[(CloudQueueMessage, DeleteOrUpdateMessage), Future[Done]] =
    AzureQueueSink.fromFunction(input => AzureQueueSinkFunctions.deleteOrUpdateMessage(cloudQueue)(input._1, input._2))
}
