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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.impl

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.stream.connectors.googlecloud.pubsub._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Keep, Sink, Source }
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, urlEqualTo }
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Base64
import javax.net.ssl.X509TrustManager
import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration._

class NoopTrustManager extends X509TrustManager {
  override def getAcceptedIssuers = new Array[X509Certificate](0)

  override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {
    // do nothing
  }

  override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {
    // do nothing
  }
}

class PubSubApiSpec extends AnyFlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers with LogCapturing
    with PubSubMockSpec {

  implicit val system: ActorSystem = ActorSystem(
    "PubSubApiSpec",
    ConfigFactory
      .parseString(
        s"pekko.connectors.google.credentials.none.project-id = ${TestCredentials.projectId}")
      .withFallback(ConfigFactory.load()))

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  it should "publish" in {

    val publishMessage =
      PublishMessage(
        data = new String(Base64.getEncoder.encode("Hello Google!".getBytes)),
        attributes = Map("row_id" -> "7"))
    val publishRequest = PublishRequest(Seq(publishMessage))

    val expectedPublishRequest =
      """{"messages":[{"data":"SGVsbG8gR29vZ2xlIQ==","attributes":{"row_id":"7"}}]}"""
    val publishResponse = """{"messageIds":["1"]}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(s"/v1/projects/${TestCredentials.projectId}/topics/topic1:publish?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(expectedPublishRequest))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(publishResponse)
            .withHeader("Content-Type", "application/json")))
    val flow = TestHttpApi.publish[Unit]("topic1", 1)
    val result =
      Source.single((publishRequest, ())).via(flow).toMat(Sink.head)(Keep.right).run()
    result.futureValue._1.messageIds shouldBe Seq("1")
    result.futureValue._2 shouldBe (())
  }

  it should "publish with ordering key" in {

    val publishMessage =
      PublishMessage(
        data = new String(Base64.getEncoder.encode("Hello Google!".getBytes)),
        attributes = Some(Map("row_id" -> "7")),
        orderingKey = Some("my-ordering-key"))
    val publishRequest = PublishRequest(Seq(publishMessage))

    val expectedPublishRequest =
      """{"messages":[{"data":"SGVsbG8gR29vZ2xlIQ==","attributes":{"row_id":"7"},"orderingKey":"my-ordering-key"}]}"""
    val publishResponse = """{"messageIds":["1"]}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(s"/v1/projects/${TestCredentials.projectId}/topics/topic1:publish?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(expectedPublishRequest))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(publishResponse)
            .withHeader("Content-Type", "application/json")))
    val flow = TestHttpApi.publish[Unit]("topic1", 1)
    val result =
      Source.single((publishRequest, ())).via(flow).toMat(Sink.head)(Keep.right).run()
    result.futureValue._1.messageIds shouldBe Seq("1")
    result.futureValue._2 shouldBe (())
  }

  it should "publish to overridden host" in {
    val httpApiWithHostToOverride = new PubSubApi {
      val isEmulated = false
      val PubSubGoogleApisHost = "invalid-host" // this host must be override to complete the test
      val PubSubGoogleApisPort = wiremockServer.httpsPort()
    }

    val publishMessage =
      PublishMessage(
        data = new String(Base64.getEncoder.encode("Hello Google!".getBytes)))

    val publishRequest = PublishRequest(Seq(publishMessage))

    val expectedPublishRequest =
      """{"messages":[{"data":"SGVsbG8gR29vZ2xlIQ=="}]}"""
    val publishResponse = """{"messageIds":["1"]}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(s"/v1/projects/${TestCredentials.projectId}/topics/topic1:publish?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(expectedPublishRequest))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(publishResponse)
            .withHeader("Content-Type", "application/json")))
    val flow = httpApiWithHostToOverride.publish[Unit]("topic1", 1, Some("localhost"))
    val result =
      Source.single((publishRequest, ())).via(flow).toMat(Sink.head)(Keep.right).run()
    result.futureValue._1.messageIds shouldBe Seq("1")
    result.futureValue._2 shouldBe (())
  }

  it should "publish without Authorization header to emulator" in {

    val publishMessage =
      PublishMessage(data = new String(Base64.getEncoder.encode("Hello Google!".getBytes)))
    val publishRequest = PublishRequest(Seq(publishMessage))

    val expectedPublishRequest =
      """{"messages":[{"data":"SGVsbG8gR29vZ2xlIQ=="}]}"""
    val publishResponse = """{"messageIds":["1"]}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(s"/v1/projects/${TestCredentials.projectId}/topics/topic1:publish?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(expectedPublishRequest))
        .withHeader("Authorization", WireMock.absent())
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(publishResponse)
            .withHeader("Content-Type", "application/json")))

    val flow = TestEmulatorHttpApi.publish[Unit]("topic1", 1)
    val result =
      Source.single((publishRequest, ())).via(flow).toMat(Sink.last)(Keep.right).run()
    result.futureValue._1.messageIds shouldBe Seq("1")
    result.futureValue._2 shouldBe (())
  }

  it should "Pull with results" in {

    val ts = Instant.parse("2019-07-04T08:10:00.111Z")

    val message =
      PubSubMessage(messageId = "1",
        data = Some(new String(Base64.getEncoder.encode("Hello Google!".getBytes))),
        publishTime = ts)

    val pullResponse =
      """{"receivedMessages":[{"ackId":"ack1","message":{"data":"SGVsbG8gR29vZ2xlIQ==","messageId":"1","publishTime":"2019-07-04T08:10:00.111Z"}}]}"""

    val pullRequest = """{"returnImmediately":true,"maxMessages":1000}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:pull?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(pullRequest))
        .willReturn(aResponse().withStatus(200).withBody(pullResponse).withHeader("Content-Type", "application/json")))

    val flow = TestHttpApi.pull("sub1", true, 1000)
    val result = Source.single(Done).via(flow).toMat(Sink.last)(Keep.right).run()
    result.futureValue shouldBe PullResponse(Some(Seq(ReceivedMessage("ack1", message))))

  }

  it should "Pull with results with ordering key" in {

    val ts = Instant.parse("2019-07-04T08:10:00.111Z")

    val message =
      PubSubMessage(messageId = "1",
        data = Some(new String(Base64.getEncoder.encode("Hello Google!".getBytes))),
        publishTime = ts,
        orderingKey = Some("my-ordering-key"))

    val pullResponse =
      """{"receivedMessages":[{"ackId":"ack1","message":{"data":"SGVsbG8gR29vZ2xlIQ==","messageId":"1","publishTime":"2019-07-04T08:10:00.111Z","orderingKey":"my-ordering-key"}}]}"""

    val pullRequest = """{"returnImmediately":true,"maxMessages":1000}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:pull?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(pullRequest))
        .willReturn(aResponse().withStatus(200).withBody(pullResponse).withHeader("Content-Type", "application/json")))

    val flow = TestHttpApi.pull("sub1", true, 1000)
    val result = Source.single(Done).via(flow).toMat(Sink.last)(Keep.right).run()
    result.futureValue shouldBe PullResponse(Some(Seq(ReceivedMessage("ack1", message))))

  }

  it should "Pull with results without access token in emulated mode" in {

    val ts = Instant.parse("2019-07-04T08:10:00.111Z")

    val message =
      PubSubMessage(messageId = "1",
        data = Some(new String(Base64.getEncoder.encode("Hello Google!".getBytes))),
        publishTime = ts)

    val pullResponse =
      """{"receivedMessages":[{"ackId":"ack1","message":{"data":"SGVsbG8gR29vZ2xlIQ==","messageId":"1","publishTime":"2019-07-04T08:10:00.111Z"}}]}"""

    val pullRequest = """{"returnImmediately":true,"maxMessages":1000}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:pull?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(pullRequest))
        .withHeader("Authorization", WireMock.absent())
        .willReturn(aResponse().withStatus(200).withBody(pullResponse).withHeader("Content-Type", "application/json")))

    val flow = TestEmulatorHttpApi.pull("sub1", true, 1000)
    val result =
      Source.single(Done).via(flow).toMat(Sink.last)(Keep.right).run()
    result.futureValue shouldBe PullResponse(Some(Seq(ReceivedMessage("ack1", message))))

  }

  it should "Pull without results" in {

    val pullResponse = "{}"

    val pullRequest = """{"returnImmediately":true,"maxMessages":1000}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:pull?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(pullRequest))
        .willReturn(aResponse().withStatus(200).withBody(pullResponse).withHeader("Content-Type", "application/json")))

    val flow = TestHttpApi.pull("sub1", true, 1000)
    val result =
      Source.single(Done).via(flow).toMat(Sink.last)(Keep.right).run()
    result.futureValue shouldBe PullResponse(None)

  }

  it should "Fail pull when HTTP response is error" in {

    val pullResponse = """{"is_valid_json": true}"""

    val pullRequest = """{"returnImmediately":true,"maxMessages":1000}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:pull?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(pullRequest))
        .willReturn(aResponse().withStatus(418).withBody(pullResponse).withHeader("Content-Type", "application/json")))

    val flow = TestHttpApi.pull("sub1", true, 1000)
    val result =
      Source.single(Done).via(flow).toMat(Sink.last)(Keep.right).run()
    val failure = result.failed.futureValue
    failure.getMessage should include("418 I'm a teapot")
    failure.getMessage should include(pullResponse)
  }

  it should "acknowledge" in {
    val ackRequest = """{"ackIds":["ack1"]}"""
    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:acknowledge?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(ackRequest))
        .willReturn(aResponse().withStatus(200)))

    val acknowledgeRequest = AcknowledgeRequest("ack1")

    val flow = TestHttpApi.acknowledge("sub1")
    val result =
      Source.single(acknowledgeRequest).via(flow).toMat(Sink.last)(Keep.right).run()
    result.futureValue shouldBe Done

  }

  it should "fail acknowledge when result code is not success" in {
    val ackRequest = """{"ackIds":["ack1"]}"""
    wireMock.register(
      WireMock
        .post(
          urlEqualTo(
            s"/v1/projects/${TestCredentials.projectId}/subscriptions/sub1:acknowledge?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(ackRequest))
        .willReturn(aResponse().withStatus(401)))

    val acknowledgeRequest = AcknowledgeRequest("ack1")

    val flow = TestHttpApi.acknowledge("sub1")
    val result =
      Source.single(acknowledgeRequest).via(flow).toMat(Sink.last)(Keep.right).run()
    result.failed.futureValue.getMessage should include("401")
  }

  it should "return exception with the meaningful error message in case of not successful publish response" in {
    val publishMessage =
      PublishMessage(
        data = new String(Base64.getEncoder.encode("Hello Google!".getBytes)),
        attributes = Map("row_id" -> "7"))

    val publishRequest = PublishRequest(Seq(publishMessage))

    val expectedPublishRequest =
      """{"messages":[{"data":"SGVsbG8gR29vZ2xlIQ==","attributes":{"row_id":"7"}}]}"""

    wireMock.register(
      WireMock
        .post(
          urlEqualTo(s"/v1/projects/${TestCredentials.projectId}/topics/topic1:publish?prettyPrint=false"))
        .withRequestBody(WireMock.equalToJson(expectedPublishRequest))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("{}")
            .withHeader("Content-Type", "application/json")))

    val flow = TestHttpApi.publish[Unit]("topic1", 1)
    val result =
      Source.single((publishRequest, ())).via(flow).toMat(Sink.last)(Keep.right).run()

    val failure = result.failed.futureValue
    failure shouldBe a[RuntimeException]
    failure.getMessage should include("404")
  }

  private val httpApi = PubSubApi
  if (httpApi.PubSubEmulatorHost.isDefined) it should "honor emulator host variables" in {
    val emulatorVar = sys.props
      .get(httpApi.PubSubEmulatorHostVarName)
      .orElse(sys.env.get(httpApi.PubSubEmulatorHostVarName))

    emulatorVar.foreach { emulatorHost =>
      httpApi.isEmulated shouldBe true
      httpApi.PubSubGoogleApisHost shouldEqual emulatorHost
    }
  }

  override def afterAll(): Unit = {
    wiremockServer.stop()
    Await.result(system.terminate(), 5.seconds)
  }
}
