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

package docs.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.awsspi.PekkoHttpClient
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
// #awsRetryConfiguration
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.retries.DefaultRetryStrategy
// #awsRetryConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import org.scalatest.wordspec.AnyWordSpecLike

class RetrySpec
    extends TestKit(ActorSystem("RetrySpec"))
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with LogCapturing {

  // #clientRetryConfig
  implicit val client: DynamoDbAsyncClient = DynamoDbAsyncClient
    .builder()
    .region(Region.AWS_GLOBAL)
    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
    .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
    // #awsRetryConfiguration
    .overrideConfiguration(
      ClientOverrideConfiguration
        .builder()
        .retryStrategy(
          // See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/retries/api/RetryStrategy.html
          DefaultRetryStrategy.legacyStrategyBuilder()
            .treatAsThrottling(_ => true)
            .build())
        .build())
    // #awsRetryConfiguration
    .build()
  // #clientRetryConfig

  override def afterAll(): Unit = {
    client.close()
    shutdown()
  }

}
