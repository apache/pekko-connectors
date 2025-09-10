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

package org.apache.pekko.stream.connectors.dynamodb.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.dynamodb.{ scaladsl, DynamoDbOp, DynamoDbPaginatedOp }
import pekko.stream.javadsl.{ Flow, FlowWithContext, Sink, Source }
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.{ DynamoDbRequest, DynamoDbResponse }

import scala.util.Try

/**
 * Factory of DynamoDb Pekko Stream operators.
 */
object DynamoDb {

  /**
   * Create a Flow that emits a response for every request.
   *
   * @param parallelism maximum number of in-flight requests at any given time
   */
  def flow[In <: DynamoDbRequest, Out <: DynamoDbResponse](client: DynamoDbAsyncClient,
      operation: DynamoDbOp[In, Out],
      parallelism: Int): Flow[In, Out, NotUsed] =
    scaladsl.DynamoDb.flow(parallelism)(client, operation).asJava

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
  @ApiMayChange(issue = "https://github.com/akka/alpakka/issues/1987")
  def flowWithContext[In <: DynamoDbRequest, Out <: DynamoDbResponse, Ctx](
      client: DynamoDbAsyncClient,
      operation: DynamoDbOp[In, Out],
      parallelism: Int): FlowWithContext[In, Ctx, Try[Out], Ctx, NotUsed] =
    scaladsl.DynamoDb.flowWithContext[In, Out, Ctx](parallelism)(client, operation).asJava

  /**
   * Create a Source that will emit potentially multiple responses for a given request.
   */
  def source[In <: DynamoDbRequest, Out <: DynamoDbResponse, Pub <: SdkPublisher[Out]](
      client: DynamoDbAsyncClient,
      operation: DynamoDbPaginatedOp[In, Out, Pub],
      request: In): Source[Out, NotUsed] =
    scaladsl.DynamoDb.source(request)(client, operation).asJava

  /**
   * Sends requests to DynamoDB and emits the paginated responses.
   *
   * Pagination is available for `BatchGetItem`, `ListTables`, `Query` and `Scan` requests.
   */
  def flowPaginated[In <: DynamoDbRequest, Out <: DynamoDbResponse](
      client: DynamoDbAsyncClient,
      operation: DynamoDbPaginatedOp[In, Out, _]): Flow[In, Out, NotUsed] =
    scaladsl.DynamoDb.flowPaginated()(client, operation).asJava

  /**
   * Create a CompletionStage that will be completed with a response to a given request.
   */
  def single[In <: DynamoDbRequest, Out <: DynamoDbResponse](
      client: DynamoDbAsyncClient,
      operation: DynamoDbOp[In, Out],
      request: In,
      system: ClassicActorSystemProvider): CompletionStage[Out] = {
    val sink: Sink[Out, CompletionStage[Out]] = Sink.head()
    Source.single(request).via(flow(client, operation, 1)).runWith(sink, system)
  }

}
