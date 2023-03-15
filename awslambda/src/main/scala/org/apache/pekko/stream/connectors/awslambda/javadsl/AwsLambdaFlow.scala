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

package org.apache.pekko.stream.connectors.awslambda.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.javadsl.Flow
import software.amazon.awssdk.services.lambda.model.{ InvokeRequest, InvokeResponse }
import software.amazon.awssdk.services.lambda.LambdaAsyncClient

object AwsLambdaFlow {

  /**
   * Java API: creates a [[AwsLambdaFlowStage]] for a AWS Lambda function invocation using an [[LambdaAsyncClient]]
   */
  def create(awsLambdaClient: LambdaAsyncClient, parallelism: Int): Flow[InvokeRequest, InvokeResponse, NotUsed] =
    pekko.stream.connectors.awslambda.scaladsl.AwsLambdaFlow.apply(parallelism)(awsLambdaClient).asJava

}
