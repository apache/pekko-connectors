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

package docs.scaladsl

import org.apache.pekko
import pekko.stream.connectors.awslambda.scaladsl.AwsLambdaFlow
import pekko.stream.scaladsl.{ Sink, Source }
import software.amazon.awssdk.services.lambda.LambdaAsyncClient

object Examples {

  // #init-sys
  import org.apache.pekko.actor.ActorSystem

  implicit val system: ActorSystem = ActorSystem()
  // #init-sys

  def initClient(): Unit = {
    // #init-client
    import com.github.pjfanning.pekkohttpspi.PekkoHttpClient
    import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
    import software.amazon.awssdk.services.lambda.LambdaAsyncClient

    // Don't encode credentials in your source code!
    // see https://doc.akka.io/docs/alpakka/current/aws-shared-configuration.html
    val credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x"))
    implicit val lambdaClient: LambdaAsyncClient = LambdaAsyncClient
      .builder()
      .credentialsProvider(credentialsProvider)
      .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
      // Possibility to configure the retry policy
      // see https://doc.akka.io/docs/alpakka/current/aws-shared-configuration.html
      // .overrideConfiguration(...)
      .build()

    system.registerOnTermination(lambdaClient.close())
    // #init-client
  }

  def run()(implicit lambdaClient: LambdaAsyncClient): Unit = {
    // #run
    import software.amazon.awssdk.core.SdkBytes
    import software.amazon.awssdk.services.lambda.model.InvokeRequest

    val request = InvokeRequest
      .builder()
      .functionName("lambda-function-name")
      .payload(SdkBytes.fromUtf8String("test-payload"))
      .build()
    Source.single(request).via(AwsLambdaFlow(1)).runWith(Sink.seq)
    // #run
  }
}
