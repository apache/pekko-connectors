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

package org.apache.pekko.stream.connectors.dynamodb.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ClassicActorSystemProvider

import scala.annotation.implicitNotFound
import pekko.stream.connectors.dynamodb.{ DynamoDbOp, DynamoDbPaginatedOp }
import pekko.stream.scaladsl.{ Flow, FlowWithContext, Sink, Source }
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * Factory of DynamoDb Pekko Stream operators.
 */
object DynamoDb {

  /**
   * Create a Flow that emits a response for every request to DynamoDB.
   *
   * @param parallelism maximum number of in-flight requests at any given time
   */
  def flow[In <: DynamoDbRequest, Out <: DynamoDbResponse](
      parallelism: Int)(implicit client: DynamoDbAsyncClient, operation: DynamoDbOp[In, Out]): Flow[In, Out, NotUsed] =
    Flow[In].mapAsync(parallelism)(operation.execute(_))

  /**
   * Create a `FlowWithContext` that emits a response for every request to DynamoDB.
   * A successful response is wrapped in [[scala.util.Success]] and a failed
   * response is wrapped in [[scala.util.Failure]].
   *
   * The context is merely passed through to the emitted element.
   *
   * @param parallelism maximum number of in-flight requests at any given time
   * @tparam Ctx context (or pass-through)
   */
  def flowWithContext[In <: DynamoDbRequest, Out <: DynamoDbResponse, Ctx](
      parallelism: Int)(implicit client: DynamoDbAsyncClient,
      operation: DynamoDbOp[In, Out]): FlowWithContext[In, Ctx, Try[Out], Ctx, NotUsed] =
    FlowWithContext.fromTuples(
      Flow[(In, Ctx)]
        .mapAsync(parallelism) {
          case (in, ctx) =>
            operation
              .execute(in)
              .map[(Try[Out], Ctx)](res => (Success(res), ctx))(ExecutionContext.parasitic)
              .recover { case t => (Failure(t), ctx) }(ExecutionContext.parasitic)
        })

  /**
   * Create a Source that will emit potentially multiple responses for a given request.
   */
  def source[In <: DynamoDbRequest, Out <: DynamoDbResponse](
      request: In)(
      implicit client: DynamoDbAsyncClient, operation: DynamoDbPaginatedOp[In, Out, _]): Source[Out, NotUsed] =
    Source.fromPublisher(operation.publisher(request))

  /**
   * Sends requests to DynamoDB and emits the paginated responses.
   *
   * Pagination is available for `BatchGetItem`, `ListTables`, `Query` and `Scan` requests.
   */
  def flowPaginated[In <: DynamoDbRequest, Out <: DynamoDbResponse]()(
      implicit client: DynamoDbAsyncClient,
      operation: DynamoDbPaginatedOp[In, Out, _]): Flow[In, Out, NotUsed] = Flow[In].flatMapConcat(source(_))

  /**
   * Create a Future that will be completed with a response to a given request.
   */
  @implicitNotFound(
    "a `ClassicActorSystemProvider` is a classic or new API actor system, provide this instead of a `Materializer`")
  def single[In <: DynamoDbRequest, Out <: DynamoDbResponse](
      request: In)(implicit client: DynamoDbAsyncClient,
      operation: DynamoDbOp[In, Out],
      system: ClassicActorSystemProvider): Future[Out] =
    Source.single(request).via(flow(1)).runWith(Sink.head)
}
