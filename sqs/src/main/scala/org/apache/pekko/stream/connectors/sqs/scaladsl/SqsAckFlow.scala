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

package org.apache.pekko.stream.connectors.sqs.scaladsl

import java.util.concurrent.CompletionException

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.{ ApiMayChange, InternalApi }
import pekko.stream.FlowShape
import pekko.stream.connectors.sqs.MessageAction._
import pekko.stream.connectors.sqs.SqsAckResult._
import pekko.stream.connectors.sqs.SqsAckResultEntry._
import pekko.stream.connectors.sqs._
import pekko.stream.scaladsl.{ Flow, GraphDSL, Merge, Partition }
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.parasitic
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

/**
 * Scala API to create acknowledging SQS flows.
 */
@ApiMayChange
object SqsAckFlow {

  /**
   * creates a [[pekko.stream.scaladsl.Flow Flow]] for ack a single SQS message at a time using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def apply(queueUrl: String, settings: SqsAckSettings = SqsAckSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Flow[MessageAction, SqsAckResult, NotUsed] = {
    checkClient(sqsClient)
    Flow[MessageAction]
      .mapAsync(settings.maxInFlight) {
        case messageAction: MessageAction.Delete =>
          val request =
            DeleteMessageRequest
              .builder()
              .queueUrl(queueUrl)
              .receiptHandle(messageAction.message.receiptHandle())
              .build()

          sqsClient
            .deleteMessage(request)
            .asScala
            .map(resp => new SqsDeleteResult(messageAction, resp))(parasitic)

        case messageAction: MessageAction.ChangeMessageVisibility =>
          val request =
            ChangeMessageVisibilityRequest
              .builder()
              .queueUrl(queueUrl)
              .receiptHandle(messageAction.message.receiptHandle())
              .visibilityTimeout(messageAction.visibilityTimeout)
              .build()

          sqsClient
            .changeMessageVisibility(request)
            .asScala
            .map(resp => new SqsChangeMessageVisibilityResult(messageAction, resp))(parasitic)

        case messageAction: MessageAction.Ignore =>
          Future.successful(new SqsIgnoreResult(messageAction))
      }
  }

  /**
   * creates a [[pekko.stream.scaladsl.Flow Flow]] for ack grouped SQS messages using an [[software.amazon.awssdk.services.sqs.SqsAsyncClient]].
   */
  def grouped(queueUrl: String, settings: SqsAckGroupedSettings = SqsAckGroupedSettings.Defaults)(
      implicit sqsClient: SqsAsyncClient): Flow[MessageAction, SqsAckResultEntry, NotUsed] = {
    checkClient(sqsClient)
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val p = builder.add(Partition[MessageAction](3,
          {
            case _: Delete                  => 0
            case _: ChangeMessageVisibility => 1
            case _: Ignore                  => 2
          }))

        val merge = builder.add(Merge[SqsAckResultEntry](3))

        val mapDelete = Flow[MessageAction].collectType[Delete]
        val mapChangeMessageVisibility = Flow[MessageAction].collectType[ChangeMessageVisibility]
        val mapChangeIgnore = Flow[MessageAction].collectType[Ignore]

        p.out(0) ~> mapDelete                  ~> groupedDelete(queueUrl, settings)                  ~> merge
        p.out(1) ~> mapChangeMessageVisibility ~> groupedChangeMessageVisibility(queueUrl, settings) ~> merge
        p.out(2) ~> mapChangeIgnore            ~> Flow[Ignore].map(new SqsIgnoreResultEntry(_))      ~> merge

        FlowShape(p.in, merge.out)
      })
  }

  private def groupedDelete(queueUrl: String, settings: SqsAckGroupedSettings)(
      implicit sqsClient: SqsAsyncClient): Flow[MessageAction.Delete, SqsDeleteResultEntry, NotUsed] = {
    checkClient(sqsClient)
    Flow[MessageAction.Delete]
      .groupedWithin(settings.maxBatchSize, settings.maxBatchWait)
      .map { actions =>
        val entries = actions.zipWithIndex.map {
          case (a, i) =>
            DeleteMessageBatchRequestEntry
              .builder()
              .id(i.toString)
              .receiptHandle(a.message.receiptHandle())
              .build()
        }

        actions -> DeleteMessageBatchRequest
          .builder()
          .queueUrl(queueUrl)
          .entries(entries.asJava)
          .build()
      }
      .mapAsync(settings.concurrentRequests) {
        case (actions: immutable.Seq[Delete], request) =>
          sqsClient
            .deleteMessageBatch(request)
            .asScala
            .map {
              case response if response.failed().isEmpty =>
                val responseMetadata = response.responseMetadata()
                val resultEntries = response.successful().asScala.map(e => e.id.toInt -> e).toMap
                actions.zipWithIndex.map {
                  case (a, i) =>
                    val result = resultEntries(i)
                    new SqsDeleteResultEntry(a, result, responseMetadata)
                }
              case resp =>
                val numberOfMessages = request.entries().size()
                val nrOfFailedMessages = resp.failed().size()
                throw new SqsBatchException(
                  numberOfMessages,
                  s"Some messages are failed to delete. $nrOfFailedMessages of $numberOfMessages messages are failed")
            }(parasitic)
            .recoverWith {
              case e: CompletionException =>
                Future.failed(new SqsBatchException(request.entries().size(), e.getMessage, e.getCause))
              case e =>
                Future.failed(new SqsBatchException(request.entries().size(), e.getMessage, e))
            }(parasitic)
      }
      .mapConcat(identity)
  }

  private def groupedChangeMessageVisibility(queueUrl: String, settings: SqsAckGroupedSettings)(
      implicit sqsClient: SqsAsyncClient)
      : Flow[MessageAction.ChangeMessageVisibility, SqsChangeMessageVisibilityResultEntry, NotUsed] =
    Flow[MessageAction.ChangeMessageVisibility]
      .groupedWithin(settings.maxBatchSize, settings.maxBatchWait)
      .map { actions =>
        val entries = actions.zipWithIndex.map {
          case (a, i) =>
            ChangeMessageVisibilityBatchRequestEntry
              .builder()
              .id(i.toString)
              .receiptHandle(a.message.receiptHandle())
              .visibilityTimeout(a.visibilityTimeout)
              .build()
        }

        actions -> ChangeMessageVisibilityBatchRequest
          .builder()
          .queueUrl(queueUrl)
          .entries(entries.asJava)
          .build()
      }
      .mapAsync(settings.concurrentRequests) {
        case (actions, request) =>
          sqsClient
            .changeMessageVisibilityBatch(request)
            .asScala
            .map {
              case response if response.failed().isEmpty =>
                val responseMetadata = response.responseMetadata()
                val resultEntries = response.successful().asScala.map(e => e.id.toInt -> e).toMap
                actions.zipWithIndex.map {
                  case (a, i) =>
                    val result = resultEntries(i)
                    new SqsChangeMessageVisibilityResultEntry(a, result, responseMetadata)
                }
              case resp =>
                val numberOfMessages = request.entries().size()
                val nrOfFailedMessages = resp.failed().size()
                throw new SqsBatchException(
                  numberOfMessages,
                  s"Some messages are failed to change visibility. $nrOfFailedMessages of $numberOfMessages messages are failed")
            }(parasitic)
            .recoverWith {
              case e: CompletionException =>
                Future.failed(new SqsBatchException(request.entries().size(), e.getMessage, e.getCause))
              case e =>
                Future.failed(new SqsBatchException(request.entries().size(), e.getMessage, e))
            }(parasitic)
      }
      .mapConcat(identity)

  @InternalApi
  private[scaladsl] def checkClient(sqsClient: SqsAsyncClient): Unit =
    require(sqsClient != null, "The `SqsAsyncClient` passed in may not be null.")
}
