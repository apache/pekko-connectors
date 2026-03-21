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

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CredentialsSpec
    extends TestKit(ActorSystem("CredentialsSpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  "Credentials" should {

    "parse 'none' provider" in {
      val config = ConfigFactory.parseString(
        """
          |provider = none
          |scopes = []
          |default-scopes = []
          |none {
          |  project-id = "test-project"
          |}
          |service-account {
          |  project-id = ""
          |  client-email = ""
          |  private-key = ""
          |  path = "/tmp/fake"
          |}
          |compute-engine.timeout = 1s
          |user-access {
          |  project-id = ""
          |  client-id = ""
          |  client-secret = ""
          |  refresh-token = ""
          |  path = "/tmp/fake"
          |}
          |access-token {
          |  project-id = ""
          |  token = ""
          |}
          |google-application-default {
          |  project-id = ""
          |}
        """.stripMargin)

      val credentials = Credentials(config)
      credentials shouldBe a[NoCredentials]
      credentials.projectId shouldBe "test-project"
    }

    "parse 'access-token' provider" in {
      val config = ConfigFactory.parseString(
        """
          |provider = access-token
          |scopes = []
          |default-scopes = []
          |none {
          |  project-id = ""
          |}
          |service-account {
          |  project-id = ""
          |  client-email = ""
          |  private-key = ""
          |  path = "/tmp/fake"
          |}
          |compute-engine.timeout = 1s
          |user-access {
          |  project-id = ""
          |  client-id = ""
          |  client-secret = ""
          |  refresh-token = ""
          |  path = "/tmp/fake"
          |}
          |access-token {
          |  project-id = "token-project"
          |  token = "my-token"
          |}
          |google-application-default {
          |  project-id = ""
          |}
        """.stripMargin)

      val credentials = Credentials(config)
      credentials shouldBe a[AccessTokenCredentials]
      credentials.projectId shouldBe "token-project"
    }

    "parse 'google-application-default' provider" in {
      val config = ConfigFactory.parseString(
        """
          |provider = google-application-default
          |scopes = ["https://www.googleapis.com/auth/cloud-platform"]
          |default-scopes = []
          |none {
          |  project-id = ""
          |}
          |service-account {
          |  project-id = ""
          |  client-email = ""
          |  private-key = ""
          |  path = "/tmp/fake"
          |}
          |compute-engine.timeout = 1s
          |user-access {
          |  project-id = ""
          |  client-id = ""
          |  client-secret = ""
          |  refresh-token = ""
          |  path = "/tmp/fake"
          |}
          |access-token {
          |  project-id = ""
          |  token = ""
          |}
          |google-application-default {
          |  project-id = "gad-project"
          |}
        """.stripMargin)

      // When application-default credentials are available (e.g. via `gcloud auth application-default login`),
      // this succeeds. In CI/test environments without credentials, getApplicationDefault() throws IOException.
      try {
        val credentials = Credentials(config)
        credentials shouldBe a[GoogleApplicationDefaultCredentials]
        credentials.projectId shouldBe "gad-project"
      } catch {
        case ex: java.io.IOException =>
          ex.getMessage should include("Application Default Credentials")
      }
    }
  }
}
