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
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.awslambda.javadsl.AwsLambdaFlow;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.*;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsLambdaFlowTest {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;
  private static LambdaAsyncClient awsLambdaClient;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
    awsLambdaClient = mock(LambdaAsyncClient.class);
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @After
  public void checkForStageLeaks() {
    StreamTestKit.assertAllStagesStopped(org.apache.pekko.stream.Materializer.matFromSystem(system));
  }

  @Test
  public void lambdaFlow() throws Exception {
    InvokeRequest invokeRequest = InvokeRequest.builder().build();
    InvokeResponse invokeResponse = InvokeResponse.builder().build();
    when(awsLambdaClient.invoke(eq(invokeRequest)))
        .thenAnswer(
            invocation -> {
              return CompletableFuture.completedFuture(invokeResponse);
            });
    Flow<InvokeRequest, InvokeResponse, NotUsed> flow = AwsLambdaFlow.create(awsLambdaClient, 1);
    Source<InvokeRequest, NotUsed> source = Source.single(invokeRequest);
    final CompletionStage<List<InvokeResponse>> stage =
        source.via(flow).runWith(Sink.seq(), system);
    assertEquals(1, stage.toCompletableFuture().get(3, TimeUnit.SECONDS).size());
  }
}
