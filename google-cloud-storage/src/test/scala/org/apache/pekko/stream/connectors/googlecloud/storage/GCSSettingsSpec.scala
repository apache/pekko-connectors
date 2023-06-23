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
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.util.ccompat.JavaConverters._
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GCSSettingsSpec extends AnyFlatSpec with Matchers with LogCapturing {
  "GCSSettings" should "create settings from application config" in {
    val endpointUrl = "https://storage.googleapis.com/"
    val basePath = "/storage/v1"

    val config = ConfigFactory.parseMap(
      Map(
        "endpoint-url" -> endpointUrl,
        "base-path" -> basePath).asJava)

    val settings = GCSSettings(config)

    settings.endpointUrl shouldBe endpointUrl
    settings.basePath shouldBe basePath
  }

}
