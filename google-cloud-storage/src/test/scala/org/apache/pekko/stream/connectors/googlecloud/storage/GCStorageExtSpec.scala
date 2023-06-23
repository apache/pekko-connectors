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

package org.apache.pekko.stream.connectors.googlecloud.storage

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.util.ccompat.JavaConverters._
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

class GCStorageExtSpec extends AnyFlatSpec with Matchers with LogCapturing {
  "GCStorageExt" should "reuse application config from actor system" in {
    val projectId = "projectId"
    val clientEmail = "clientEmail"
    val privateKey = "privateKey"
    val baseUrl = "http://base"
    val basePath = "/path"
    val tokenUrl = "http://token"
    val tokenScope = "everything"

    val config = ConfigFactory.parseMap(
      Map(
        "pekko.connectors.google.cloud.storage.project-id" -> projectId,
        "pekko.connectors.google.cloud.storage.client-email" -> clientEmail,
        "pekko.connectors.google.cloud.storage.private-key" -> privateKey,
        "pekko.connectors.google.cloud.storage.base-url" -> baseUrl,
        "pekko.connectors.google.cloud.storage.base-path" -> basePath,
        "pekko.connectors.google.cloud.storage.token-url" -> tokenUrl,
        "pekko.connectors.google.cloud.storage.token-scope" -> tokenScope).asJava)
    implicit val system = ActorSystem.create("gcStorage", config)
    @nowarn("msg=deprecated")
    val ext = GCStorageExt(system)

    ext.settings.projectId shouldBe projectId
    ext.settings.clientEmail shouldBe clientEmail
    ext.settings.privateKey shouldBe privateKey
    ext.settings.baseUrl shouldBe baseUrl
    ext.settings.basePath shouldBe basePath
    ext.settings.tokenUrl shouldBe tokenUrl
    ext.settings.tokenScope shouldBe tokenScope
  }
}
