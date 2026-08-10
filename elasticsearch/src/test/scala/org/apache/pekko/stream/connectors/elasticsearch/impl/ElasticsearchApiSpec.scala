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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ElasticsearchApiSpec
    extends TestKit(ActorSystem("elasticsearch-api-spec"))
    with AnyWordSpecLike
    with Matchers
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

      val ex = intercept[IllegalStateException] {
        ElasticsearchApi.executeRequest(request, connectionSettings)
      }
      ex.getMessage should include("not 'https'")
      ex.getMessage should include("insecure")
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
