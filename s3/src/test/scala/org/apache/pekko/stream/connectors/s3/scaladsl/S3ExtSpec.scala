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

package org.apache.pekko.stream.connectors.s3.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.s3.AccessStyle.PathAccessStyle
import pekko.stream.connectors.s3.S3Ext
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters._

class S3ExtSpec extends AnyFlatSpecLike with Matchers {
  it should "reuse application config from actor system" in {
    val config = ConfigFactory.parseMap(
      Map(
        "pekko.connectors.s3.endpoint-url" -> "http://localhost:8001",
        "pekko.connectors.s3.path-style-access" -> true).asJava)
    implicit val system: ActorSystem = ActorSystem.create("s3", config)
    val ext = S3Ext(system)
    ext.settings.endpointUrl shouldBe Some("http://localhost:8001")
    ext.settings.accessStyle shouldBe PathAccessStyle
  }
}
