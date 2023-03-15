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

package docs.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.{ Http, HttpExt }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterAll, Inspectors }

trait ElasticsearchSpecBase
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Inspectors
    with LogCapturing
    with BeforeAndAfterAll {

  // #init-mat
  implicit val system: ActorSystem = ActorSystem()
  // #init-mat
  implicit val http: HttpExt = Http()
}
