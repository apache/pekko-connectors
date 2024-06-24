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

package docs.javadsl;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.awsspi.PekkoHttpClient;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Rule;
import org.junit.Test;
// #clientRetryConfig
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
// #awsRetryConfiguration
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
// #clientRetryConfig

// #awsRetryConfiguration

public class RetryTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @Test
  public void setup() throws Exception {
    final ActorSystem system = ActorSystem.create();
    // #clientRetryConfig
    final DynamoDbAsyncClient client =
        DynamoDbAsyncClient.builder()
            .region(Region.AWS_GLOBAL)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
            .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
            // #awsRetryConfiguration
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .retryStrategy(
                        // See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/retries/api/RetryStrategy.html
                        DefaultRetryStrategy.legacyStrategyBuilder()
                            .treatAsThrottling(e -> true)
                            .build())
                    .build())
            // #awsRetryConfiguration
            .build();
    system.registerOnTermination(client::close);
    // #clientRetryConfig

    client.close();
    TestKit.shutdownActorSystem(system);
  }
}
