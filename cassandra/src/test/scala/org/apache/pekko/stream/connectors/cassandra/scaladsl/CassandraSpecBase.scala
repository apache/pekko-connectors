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

package org.apache.pekko.stream.connectors.cassandra.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.testkit.TestKit
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }

import scala.concurrent.ExecutionContext
import pekko.stream.{ Materializer, SystemMaterializer }

/**
 * All the tests must be run with a local Cassandra running on default port 9042.
 */
abstract class CassandraSpecBase(_system: ActorSystem)
    extends TestKit(_system)
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with CassandraLifecycle
    with LogCapturing {

  implicit val materializer: Materializer = SystemMaterializer(_system).materializer
  implicit val ec: ExecutionContext = system.dispatcher

  lazy val sessionRegistry: CassandraSessionRegistry = CassandraSessionRegistry(system)

}
