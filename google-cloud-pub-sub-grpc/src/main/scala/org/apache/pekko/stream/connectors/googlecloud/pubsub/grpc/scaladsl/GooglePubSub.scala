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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl

import org.apache.pekko
import pekko.actor.Cancellable
import pekko.dispatch.ExecutionContexts
import pekko.stream.{ Attributes, Materializer }
import pekko.stream.scaladsl.{ Flow, Keep, Sink, Source }
import pekko.{ Done, NotUsed }
import com.google.pubsub.v1.pubsub._

import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

/**
 * Google Pub/Sub Pekko Stream operator factory.
 */
object GooglePubSub {

  /**
   * Create a flow to publish messages to Google Cloud Pub/Sub. The flow emits responses that contain published
   * message ids.
   *
   * @param parallelism controls how many messages can be in-flight at any given time
   */
  def publish(parallelism: Int): Flow[PublishRequest, PublishResponse, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow[PublishRequest]
          .mapAsyncUnordered(parallelism)(publisher(mat, attr).client.publish)
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a source that emits messages for a given subscription using a StreamingPullRequest.
   *
   * The materialized value can be used to cancel the source.
   *
   * @param request the subscription FQRS and ack deadline fields are mandatory for the request
   * @param pollInterval time between StreamingPullRequest messages are being sent
   */
  def subscribe(
      request: StreamingPullRequest,
      pollInterval: FiniteDuration): Source[ReceivedMessage, Future[Cancellable]] =
    Source
      .fromMaterializer { (mat, attr) =>
        val cancellable = Promise[Cancellable]()

        val subsequentRequest = request
          .withSubscription("")
          .withStreamAckDeadlineSeconds(0)

        subscriber(mat, attr).client
          .streamingPull(
            Source
              .single(request)
              .concat(
                Source
                  .tick(0.seconds, pollInterval, ())
                  .map(_ => subsequentRequest)
                  .mapMaterializedValue(cancellable.success)))
          .mapConcat(_.receivedMessages.toVector)
          .mapMaterializedValue(_ => cancellable.future)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContexts.parasitic))

  /**
   * Create a source that emits messages for a given subscription using a synchronous PullRequest.
   *
   * The materialized value can be used to cancel the source.
   *
   * @param request the subscription FQRS field is mandatory for the request
   * @param pollInterval time between PullRequest messages are being sent
   */
  def subscribePolling(
      request: PullRequest,
      pollInterval: FiniteDuration): Source[ReceivedMessage, Future[Cancellable]] =
    Source
      .fromMaterializer { (mat, attr) =>
        val cancellable = Promise[Cancellable]()
        val client = subscriber(mat, attr).client
        Source
          .tick(0.seconds, pollInterval, request)
          .mapMaterializedValue(cancellable.success)
          .mapAsync(1)(client.pull(_))
          .mapConcat(_.receivedMessages.toVector)
          .mapMaterializedValue(_ => cancellable.future)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContexts.parasitic))

  /**
   * Create a flow that accepts consumed message acknowledgements.
   */
  def acknowledgeFlow(): Flow[AcknowledgeRequest, AcknowledgeRequest, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsync(1)(req =>
            subscriber(mat, attr).client
              .acknowledge(req)
              .map(_ => req)(mat.executionContext))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a sink that accepts consumed message acknowledgements.
   *
   * The materialized value completes on stream completion.
   *
   * @param parallelism controls how many acknowledgements can be in-flight at any given time
   */
  def acknowledge(parallelism: Int): Sink[AcknowledgeRequest, Future[Done]] = {
    Sink
      .fromMaterializer { (mat, attr) =>
        Flow[AcknowledgeRequest]
          .mapAsyncUnordered(parallelism)(subscriber(mat, attr).client.acknowledge)
          .toMat(Sink.ignore)(Keep.right)
      }
      .mapMaterializedValue(_.flatMap(identity)(ExecutionContexts.parasitic))
  }

  private def publisher(mat: Materializer, attr: Attributes) =
    attr
      .get[PubSubAttributes.Publisher]
      .map(_.publisher)
      .getOrElse(GrpcPublisherExt()(mat.system).publisher)

  private def subscriber(mat: Materializer, attr: Attributes) =
    attr
      .get[PubSubAttributes.Subscriber]
      .map(_.subscriber)
      .getOrElse(GrpcSubscriberExt()(mat.system).subscriber)
}
