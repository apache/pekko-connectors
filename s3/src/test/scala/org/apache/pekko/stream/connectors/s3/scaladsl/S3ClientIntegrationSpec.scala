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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.testkit.TestKit
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

trait S3ClientIntegrationSpec
    extends AnyFlatSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with LogCapturing {

  implicit val system: ActorSystem

  override protected def afterAll(): Unit = {
    Http(system)
      .shutdownAllConnectionPools()
      .foreach(_ => TestKit.shutdownActorSystem(system))(system.dispatcher)
  }

}
