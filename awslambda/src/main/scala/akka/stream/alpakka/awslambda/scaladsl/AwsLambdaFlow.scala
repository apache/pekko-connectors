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

package akka.stream.alpakka.awslambda.scaladsl

import akka.NotUsed
import akka.stream.scaladsl.Flow
import software.amazon.awssdk.services.lambda.model.{ InvokeRequest, InvokeResponse }
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import scala.compat.java8.FutureConverters._

object AwsLambdaFlow {

  /**
   * Scala API: creates a [[AwsLambdaFlowStage]] for a AWS Lambda function invocation using [[LambdaAsyncClient]]
   */
  def apply(
      parallelism: Int)(implicit awsLambdaClient: LambdaAsyncClient): Flow[InvokeRequest, InvokeResponse, NotUsed] =
    Flow[InvokeRequest].mapAsyncUnordered(parallelism)(awsLambdaClient.invoke(_).toScala)

}
