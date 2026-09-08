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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.{ Http, HttpExt }
import pekko.http.scaladsl.model._
import pekko.stream.Materializer
import pekko.stream.connectors.elasticsearch.ElasticsearchConnectionSettings
import pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ElasticsearchApiSpec
    extends TestKit(ActorSystem("elasticsearch-api-spec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  implicit val mat: Materializer = Materializer(system)
  implicit val http: HttpExt = Http()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ElasticsearchApi.executeRequest" should {

    "reject plain HTTP requests when credentials are configured" in {
      val connectionSettings =
        ElasticsearchConnectionSettings("http://localhost:9200")
          .withCredentials("user", "pass")

      val request = HttpRequest(HttpMethods.GET)
        .withUri(Uri("http://localhost:9200/_search"))

      // the rejection arrives as a failed Future, not as a synchronous throw,
      // so that the calling stages can route it through their failure handling
      noException should be thrownBy ElasticsearchApi.executeRequest(request, connectionSettings)

      val ex = ElasticsearchApi
        .executeRequest(request, connectionSettings)
        .failed
        .futureValue
      ex shouldBe an[IllegalStateException]
      ex.getMessage should include("not 'https'")
      ex.getMessage should include("insecure")
    }

    "interpolate the actual scheme into the rejection message" in {
      val connectionSettings =
        ElasticsearchConnectionSettings("http://localhost:9200")
          .withCredentials("user", "pass")

      val request = HttpRequest(HttpMethods.GET)
        .withUri(Uri("http://localhost:9200/_search"))

      val ex = ElasticsearchApi
        .executeRequest(request, connectionSettings)
        .failed
        .futureValue
      ex.getMessage should include("scheme is 'http'")
      (ex.getMessage should not).include("%s")
    }

  }

  "ElasticsearchApi.checkCredentialsTransport" should {

    val withCredentials =
      ElasticsearchConnectionSettings("http://localhost:9200").withCredentials("user", "pass")

    "reject a plain HTTP scheme by default" in {
      val result = ElasticsearchApi.checkCredentialsTransport("http", withCredentials)
      result.isLeft shouldBe true
      result.left.toOption.get should include("scheme is 'http'")
    }

    "accept an HTTPS scheme without warning" in {
      ElasticsearchApi.checkCredentialsTransport("https", withCredentials) shouldBe Right(None)
    }

    "accept an HTTPS scheme regardless of case" in {
      ElasticsearchApi.checkCredentialsTransport("HTTPS", withCredentials) shouldBe Right(None)
    }

    "accept a plain HTTP scheme with a warning when insecure transport is allowed" in {
      val settings = withCredentials.withAllowInsecureCredentialsTransport(true)
      val result = ElasticsearchApi.checkCredentialsTransport("http", settings)
      result.isRight shouldBe true
      val warning = result.toOption.get
      warning should be(defined)
      warning.get should include("insecure")
    }

    "allow HTTPS requests when credentials are configured" in {
      val connectionSettings =
        ElasticsearchConnectionSettings("https://localhost:9200")
          .withCredentials("user", "pass")

      val request = HttpRequest(HttpMethods.GET)
        .withUri(Uri("https://localhost:9200/_search"))

      // Validation passes — no IllegalStateException thrown synchronously.
      // The future will fail at the network level (no server), which is expected.
      noException should be thrownBy ElasticsearchApi.executeRequest(request, connectionSettings)
    }

    "allow plain HTTP requests when no credentials are configured" in {
      val connectionSettings =
        ElasticsearchConnectionSettings("http://localhost:9200")

      val request = HttpRequest(HttpMethods.GET)
        .withUri(Uri("http://localhost:9200/_search"))

      // No credentials means no HTTPS enforcement.
      // The future will fail at the network level (no server), which is expected.
      noException should be thrownBy ElasticsearchApi.executeRequest(request, connectionSettings)
    }
  }
}
