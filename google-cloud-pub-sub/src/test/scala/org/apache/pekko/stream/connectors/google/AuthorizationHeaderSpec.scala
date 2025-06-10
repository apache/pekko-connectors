/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.google

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.headers.OAuth2BearerToken
import pekko.stream.connectors.googlecloud.pubsub.impl.TestCredentials
import pekko.stream.connectors.googlecloud.pubsub.{ PubSubMockSpec, PublishMessage, PublishRequest }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.connectors.google.auth.{ Credentials, RetrievableCredentials }
import pekko.stream.scaladsl.{ Keep, Sink, Source }
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, urlEqualTo }
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ when, withSettings }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.util.Base64
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.immutable.Seq
import scala.concurrent.duration._

class AuthorizationHeaderSpec extends AnyFlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers
    with LogCapturing
    with PubSubMockSpec with MockitoSugar {

  implicit val system: ActorSystem = ActorSystem(
    "AuthorizationHeaderSpec",
    ConfigFactory
      .parseString(
        s"pekko.connectors.google.credentials.none.project-id = ${TestCredentials.projectId}")
      .withFallback(ConfigFactory.load()))

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  it should "publish with auth header" in {

    val accessToken = "yyyy.c.an-access-token"
    val credentials =
      mock[Credentials with RetrievableCredentials](withSettings().extraInterfaces(classOf[RetrievableCredentials]))

    when(credentials.get()(any[ExecutionContext], any[RequestSettings])).thenReturn(
      Future.successful(OAuth2BearerToken(accessToken))
    )

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
        .withHeader("Authorization", WireMock.equalTo("Bearer " + accessToken))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(publishResponse)
            .withHeader("Content-Type", "application/json")))
    val flow = TestHttpApi.publish[Unit]("topic1", 1)
    val result =
      Source.single((publishRequest, ())).via(flow)
        .withAttributes(GoogleAttributes.settings(GoogleSettings().copy(credentials = credentials)))
        .toMat(Sink.head)(Keep.right).run()
    result.futureValue._1.messageIds shouldBe Seq("1")
    result.futureValue._2 shouldBe (())
  }
}
