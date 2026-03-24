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

import org.apache.pekko.stream.connectors.google.RequestSettings
import com.google.auth.oauth2.{ GoogleCredentials => OAuthGoogleCredentials }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class ProvidedCredentialsSpec extends AnyWordSpec with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val settings: RequestSettings = null // not used by ProvidedCredentials

  "ProvidedCredentials" should {

    "wrap user-provided credentials and return them via asGoogle" in {
      val googleCreds = OAuthGoogleCredentials.newBuilder().build()
      val provided = ProvidedCredentials("my-project", googleCreds)

      provided.projectId shouldBe "my-project"
      provided.asGoogle shouldBe googleCreds
    }

    "expose projectId via Java API" in {
      val googleCreds = OAuthGoogleCredentials.newBuilder().build()
      val provided = ProvidedCredentials.create("java-project", googleCreds)

      provided.getProjectId shouldBe "java-project"
    }

    "use Scala apply factory method" in {
      val googleCreds = OAuthGoogleCredentials.newBuilder().build()
      val provided = ProvidedCredentials("scala-project", googleCreds)

      provided shouldBe a[ProvidedCredentials]
      provided.projectId shouldBe "scala-project"
    }
  }
}
