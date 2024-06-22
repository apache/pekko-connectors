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

import org.apache.pekko.NotUsed;
// #init-sys
import org.apache.pekko.actor.ActorSystem;
// #init-sys
import org.apache.pekko.stream.connectors.awslambda.javadsl.AwsLambdaFlow;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
// #init-client
import org.apache.pekko.stream.connectors.awsspi.PekkoHttpClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
// #init-client
// #run
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.core.SdkBytes;
// #run

import java.util.List;
import java.util.concurrent.CompletionStage;

public class Examples {

  // #init-sys

  ActorSystem system = ActorSystem.create();
  // #init-sys

  public void initClient() {
    // #init-client

    // Don't encode credentials in your source code!
    // see https://pekko.apache.org/docs/pekko-connectors/current/aws-shared-configuration.html
    StaticCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x"));
    LambdaAsyncClient awsLambdaClient =
        LambdaAsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
            // Possibility to configure the retry policy
            // see https://pekko.apache.org/docs/pekko-connectors/current/aws-shared-configuration.html
            // .overrideConfiguration(...)
            .build();

    system.registerOnTermination(awsLambdaClient::close);
    // #init-client
  }

  public void run(LambdaAsyncClient awsLambdaClient) {
    // #run

    InvokeRequest request =
        InvokeRequest.builder()
            .functionName("lambda-function-name")
            .payload(SdkBytes.fromUtf8String("test-payload"))
            .build();
    Flow<InvokeRequest, InvokeResponse, NotUsed> flow = AwsLambdaFlow.create(awsLambdaClient, 1);
    final CompletionStage<List<InvokeResponse>> stage =
        Source.single(request).via(flow).runWith(Sink.seq(), system);
    // #run
  }
}
