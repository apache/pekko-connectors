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

package org.apache.pekko.stream.connectors.google

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

class GoogleSettingsSpec
    extends TestKit(ActorSystem("GoogleSettingsSpec"))
    with AnyFlatSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  private def mkRequestSettings(more: String): RequestSettings =
    RequestSettings(
      ConfigFactory
        .parseString(
          s"""
             |user-ip = ""
             |quota-user = ""
             |pretty-print = false
             |
             |upload-chunk-size = 15 MiB
             |
             |retry-settings {
             |  max-retries = 6
             |  min-backoff = 1 second
             |  max-backoff = 1 minute
             |  random-factor = 0.2
             |}
             |$more
        """.stripMargin)
        .resolve)

  private def mkSettings(more: String): GoogleSettings =
    GoogleSettings(
      ConfigFactory
        .parseString(more)
        .withFallback(ConfigFactory.defaultReference().getConfig(GoogleSettings.ConfigPath))
        .resolve)

  it should "prefer the project-id setting over the one from the credentials provider" in {
    mkSettings("""
                 |project-id = "explicit-project"
                 |default-project-id = "env-project"
                 |credentials {
                 |  provider = access-token
                 |  access-token {
                 |    project-id = "credentials-project"
                 |    token = "yyyy.c.an-access-token"
                 |  }
                 |}
      """.stripMargin).projectId shouldEqual "explicit-project"
  }

  it should "fall back to the project id from the credentials provider" in {
    mkSettings("""
                 |project-id = ""
                 |default-project-id = "env-project"
                 |credentials {
                 |  provider = access-token
                 |  access-token {
                 |    project-id = "credentials-project"
                 |    token = "yyyy.c.an-access-token"
                 |  }
                 |}
      """.stripMargin).projectId shouldEqual "credentials-project"
  }

  it should "fall back to default-project-id when no other project id is available" in {
    mkSettings("""
                 |project-id = ""
                 |default-project-id = "env-project"
                 |credentials {
                 |  provider = access-token
                 |  access-token {
                 |    project-id = ""
                 |    token = "yyyy.c.an-access-token"
                 |  }
                 |}
      """.stripMargin).projectId shouldEqual "env-project"
  }

  it should "leave the project id empty when nothing supplies one" in {
    mkSettings("""
                 |project-id = ""
                 |default-project-id = ""
                 |credentials {
                 |  provider = access-token
                 |  access-token {
                 |    project-id = ""
                 |    token = "yyyy.c.an-access-token"
                 |  }
                 |}
      """.stripMargin).projectId shouldEqual ""
  }

  it should "resolve the project id from a config that predates the project-id settings" in {
    val legacy = ConfigFactory
      .parseString("""
                     |credentials {
                     |  provider = access-token
                     |  access-token {
                     |    project-id = "credentials-project"
                     |    token = "yyyy.c.an-access-token"
                     |  }
                     |}
        """.stripMargin)
      .withFallback(ConfigFactory.defaultReference().getConfig(GoogleSettings.ConfigPath))
      .resolve
      .withoutPath("project-id")
      .withoutPath("default-project-id")
    GoogleSettings(legacy).projectId shouldEqual "credentials-project"
  }

  it should "skip parsing forward-proxy when optional environment overrides exist but aren't set" in {
    @nowarn("msg=possible missing interpolator: detected an interpolated expression")
    val config = """
                   |forward-proxy {
                   |  host = ${?HOST}
                   |  port = ${?PORT}
                   |  credentials {
                   |    username = ${?CREDENTIALS_USERNAME}
                   |    password = ${?CREDENTIALS_PASSWORD}
                   |  }
                   |}
                """.stripMargin
    noException should be thrownBy mkRequestSettings(config)
    mkRequestSettings(config).forwardProxy shouldEqual None
  }
}
