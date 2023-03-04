/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.storage

import akka.actor.ActorSystem
import akka.stream.alpakka.testkit.scaladsl.LogCapturing
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class GCSExtSpec extends AnyFlatSpec with Matchers with LogCapturing {
  "GCSExt" should "reuse application config from actor system" in {
    val endpointUrl = "https://storage.googleapis.com/"
    val basePath = "/storage/v1"

    val config = ConfigFactory.parseMap(
      Map(
        "alpakka.google.cloud-storage.endpoint-url" -> endpointUrl,
        "alpakka.google.cloud-storage.base-path" -> basePath).asJava)

    implicit val system = ActorSystem.create("gcs", config)
    val ext = GCSExt(system)

    ext.settings.endpointUrl shouldBe endpointUrl
    ext.settings.basePath shouldBe basePath
  }

}
