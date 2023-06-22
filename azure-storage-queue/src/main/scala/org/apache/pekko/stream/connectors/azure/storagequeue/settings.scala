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

package org.apache.pekko.stream.connectors.azure.storagequeue

import org.apache.pekko.util.OptionConverters._

import java.time.{ Duration => JavaDuration }
import java.util.Optional

import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * Settings for AzureQueueSource
 *
 * @param initalVisibilityTimeout Specifies how many seconds a message becomes invisible after it has been dequeued.
 *        See parameter of the same name in [[com.microsoft.azure.storage.queue.CloudQueue$.retrieveMessages]].
 * @param batchSize Specifies how many message are fetched in one batch.
 *        (This is the numberOfMessages parameter in [[com.microsoft.azure.storage.queue.CloudQueue$.retrieveMessages]].)
 * @param retrieveRetryTimeout If None the [[org.apache.pekko.stream.connectors.azure.storagequeue.scaladsl.AzureQueueSource]] will be completed if the queue is empty.
 *        If Some(timeout) [[org.apache.pekko.stream.connectors.azure.storagequeue.scaladsl.AzureQueueSource]] will retry after timeout to get new messages. Do not set timeout to low.
 */
final class AzureQueueSourceSettings private (
    val initialVisibilityTimeout: Int,
    val batchSize: Int,
    val retrieveRetryTimeout: Option[FiniteDuration] = None) {

  def withBatchSize(batchSize: Int): AzureQueueSourceSettings =
    copy(batchSize = batchSize)

  /**
   * @param retrieveRetryTimeout in seconds. If <= 0 retrying of message retrieval is disabled.
   * @return
   */
  def withRetrieveRetryTimeout(retrieveRetryTimeout: FiniteDuration): AzureQueueSourceSettings =
    copy(retrieveRetryTimeout = Some(retrieveRetryTimeout))

  def withRetrieveRetryTimeout(retrieveRetryTimeout: JavaDuration) =
    copy(retrieveRetryTimeout = Some(Duration.fromNanos(retrieveRetryTimeout.toNanos)))

  /**
   * Java API
   */
  def getRetrieveRetryTimeout(): Optional[JavaDuration] =
    retrieveRetryTimeout.map(d => JavaDuration.ofNanos(d.toNanos)).toJava

  private def copy(batchSize: Int = batchSize, retrieveRetryTimeout: Option[FiniteDuration] = retrieveRetryTimeout) =
    new AzureQueueSourceSettings(initialVisibilityTimeout, batchSize, retrieveRetryTimeout)

  override def toString: String =
    s"AzureQueueSourceSettings(initialVisibilityTimeout=$initialVisibilityTimeout, batchSize=$batchSize, retrieveRetryTimeout=$retrieveRetryTimeout)"
}

object AzureQueueSourceSettings {

  def apply(initialVisibilityTimeout: Int, batchSize: Int): AzureQueueSourceSettings =
    new AzureQueueSourceSettings(initialVisibilityTimeout, batchSize)

  def create(initialVisibilityTimeout: Int, batchSize: Int): AzureQueueSourceSettings =
    AzureQueueSourceSettings(initialVisibilityTimeout, batchSize)

  /**
   * Default settings
   *
   * initialVisibilityTimeout (30) is taken from
   * [[com.microsoft.azure.storage.queue.QueueConstants.DEFAULT_VISIBILITY_MESSAGE_TIMEOUT_IN_SECONDS]]
   */
  def apply(): AzureQueueSourceSettings = AzureQueueSourceSettings(30, 10)

  /**
   * Java API
   *
   * Default settings
   *
   * initialVisibilityTimeout (30) is taken from
   * [[com.microsoft.azure.storage.queue.QueueConstants.DEFAULT_VISIBILITY_MESSAGE_TIMEOUT_IN_SECONDS]]
   */
  def create(): AzureQueueSourceSettings = AzureQueueSourceSettings()
}
