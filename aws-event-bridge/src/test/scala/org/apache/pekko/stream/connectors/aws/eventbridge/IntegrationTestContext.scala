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

package org.apache.pekko.stream.connectors.aws.eventbridge

import java.util.UUID

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, Suite }
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusRequest

import scala.concurrent.duration.FiniteDuration

trait IntegrationTestContext extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  // #init-system
  implicit val system: ActorSystem = ActorSystem()
  // #init-system

  def eventBusEndpoint: String = s"http://localhost:4587"

  implicit var eventBridgeClient: EventBridgeAsyncClient = _
  var eventBusArn: String = _

  def createEventBus(): String =
    eventBridgeClient
      .createEventBus(
        CreateEventBusRequest.builder().name(s"pekko-connectors-topic-${UUID.randomUUID().toString}").build())
      .get()
      .eventBusArn()

  override protected def beforeAll(): Unit = {
    eventBridgeClient = createAsyncClient(eventBusEndpoint)
    eventBusArn = createEventBus()
  }

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  def createAsyncClient(endEndpoint: String): EventBridgeAsyncClient = {
    // #init-client
    import java.net.URI

    import pekko.stream.connectors.awsspi.PekkoHttpClient
    import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
    import software.amazon.awssdk.regions.Region
    import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient

    implicit val awsEventBridgeClient: EventBridgeAsyncClient =
      EventBridgeAsyncClient
        .builder()
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
        .endpointOverride(URI.create(endEndpoint))
        .region(Region.EU_CENTRAL_1)
        .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
        .build()

    system.registerOnTermination(awsEventBridgeClient.close())
    // #init-client
    awsEventBridgeClient
  }

  def sleep(d: FiniteDuration): Unit = Thread.sleep(d.toMillis)

}
