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

package org.apache.pekko.stream.connectors.googlecloud.storage

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.util.ccompat.JavaConverters._
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GCSExtSpec extends AnyFlatSpec with Matchers with LogCapturing {
  "GCSExt" should "reuse application config from actor system" in {
    val endpointUrl = "https://storage.googleapis.com/"
    val basePath = "/storage/v1"

    val config = ConfigFactory.parseMap(
      Map(
        "pekko.connectors.google.cloud-storage.endpoint-url" -> endpointUrl,
        "pekko.connectors.google.cloud-storage.base-path" -> basePath).asJava)

    implicit val system: ActorSystem = ActorSystem.create("gcs", config)
    val ext = GCSExt(system)

    ext.settings.endpointUrl shouldBe endpointUrl
    ext.settings.basePath shouldBe basePath
  }

}
