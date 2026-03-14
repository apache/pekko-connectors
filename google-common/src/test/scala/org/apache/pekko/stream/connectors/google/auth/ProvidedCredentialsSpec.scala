/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
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
