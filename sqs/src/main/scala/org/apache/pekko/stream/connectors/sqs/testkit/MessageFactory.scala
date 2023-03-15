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

package org.apache.pekko.stream.connectors.sqs.testkit

import org.apache.pekko
import pekko.stream.connectors.sqs.SqsAckResult.{ SqsChangeMessageVisibilityResult, SqsDeleteResult, SqsIgnoreResult }
import pekko.stream.connectors.sqs.SqsAckResultEntry.{
  SqsChangeMessageVisibilityResultEntry,
  SqsDeleteResultEntry,
  SqsIgnoreResultEntry
}
import pekko.stream.connectors.sqs.{ MessageAction, SqsPublishResult, SqsPublishResultEntry, SqsResult }
import software.amazon.awssdk.services.sqs.model._

/**
 * Message factory class for testing purposes
 */
object MessageFactory {
  def createSqsPublishResult(request: SendMessageRequest, response: SendMessageResponse): SqsPublishResult =
    new SqsPublishResult(request, response)

  def createSqsPublishResultEntry(
      request: SendMessageRequest,
      result: SendMessageBatchResultEntry,
      responseMetadata: SqsResponseMetadata = SqsResult.EmptySqsResponseMetadata): SqsPublishResultEntry =
    new SqsPublishResultEntry(request, result, responseMetadata)

  def createSqsDeleteResult(messageAction: MessageAction.Delete, response: DeleteMessageResponse): SqsDeleteResult =
    new SqsDeleteResult(messageAction, response)

  def createSqsIgnoreResult(messageAction: MessageAction.Ignore): SqsIgnoreResult =
    new SqsIgnoreResult(messageAction)

  def createSqsChangeMessageVisibilityResult(
      messageAction: MessageAction.ChangeMessageVisibility,
      response: ChangeMessageVisibilityResponse): SqsChangeMessageVisibilityResult =
    new SqsChangeMessageVisibilityResult(messageAction, response)

  def createSqsDeleteResultEntry(
      messageAction: MessageAction.Delete,
      result: DeleteMessageBatchResultEntry,
      responseMetadata: SqsResponseMetadata = SqsResult.EmptySqsResponseMetadata): SqsDeleteResultEntry =
    new SqsDeleteResultEntry(messageAction, result, responseMetadata)

  def createSqsIgnoreResultEntry(messageAction: MessageAction.Ignore): SqsIgnoreResultEntry =
    new SqsIgnoreResultEntry(messageAction)

  def createSqsChangeMessageVisibilityResultEntry(
      messageAction: MessageAction.ChangeMessageVisibility,
      result: ChangeMessageVisibilityBatchResultEntry,
      responseMetadata: SqsResponseMetadata = SqsResult.EmptySqsResponseMetadata)
      : SqsChangeMessageVisibilityResultEntry =
    new SqsChangeMessageVisibilityResultEntry(messageAction, result, responseMetadata)
}
