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

package org.apache.pekko.stream.connectors.awslambda.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.scaladsl.Flow
import pekko.util.FutureConverters._
import software.amazon.awssdk.services.lambda.model.{ InvokeRequest, InvokeResponse }
import software.amazon.awssdk.services.lambda.LambdaAsyncClient

object AwsLambdaFlow {

  /**
   * Scala API: creates a [[AwsLambdaFlow]] for a AWS Lambda function invocation using [[LambdaAsyncClient]]
   */
  def apply(
      parallelism: Int)(implicit awsLambdaClient: LambdaAsyncClient): Flow[InvokeRequest, InvokeResponse, NotUsed] =
    Flow[InvokeRequest].mapAsyncUnordered(parallelism)(awsLambdaClient.invoke(_).asScala)

}
