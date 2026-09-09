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

package docs.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.{ Http, HttpExt }
import pekko.stream.connectors.elasticsearch._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.testkit.TestKit
import org.scalatest.{ BeforeAndAfterAll, Inspectors }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ElasticsearchSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with Inspectors
    with LogCapturing
    with ElasticsearchConnectorBehaviour
    with ElasticsearchSpecUtils
    with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem()
  implicit val http: HttpExt = Http()

  val clientV5: ElasticsearchConnectionSettings =
    ElasticsearchConnectionSettings("http://localhost:9201")
  val clientV7: ElasticsearchConnectionSettings =
    ElasticsearchConnectionSettings("http://localhost:9202")
  val clientV8: ElasticsearchConnectionSettings =
    ElasticsearchConnectionSettings("http://localhost:9205")
  val clientV9: ElasticsearchConnectionSettings =
    ElasticsearchConnectionSettings("http://localhost:9206")

  override def afterAll(): Unit = {
    deleteAllIndices(clientV5)
    deleteAllIndices(clientV7)
    deleteAllIndices(clientV8)
    deleteAllIndices(clientV9)

    TestKit.shutdownActorSystem(system)
  }

  "Connector with ApiVersion 5 running against Elasticsearch v6.8.0" should {
    behave.like(elasticsearchConnector(ApiVersion.V5, clientV5))
  }

  "Connector with ApiVersion 7 running against Elasticsearch v7.6.0" should {
    behave.like(elasticsearchConnector(ApiVersion.V7, clientV7))
  }

  "Connector with ApiVersion 8 running against Elasticsearch v8" should {
    behave.like(elasticsearchConnector(ApiVersion.V8, clientV8))
  }

  "Connector with ApiVersion 9 running against Elasticsearch v9" should {
    behave.like(elasticsearchConnector(ApiVersion.V9, clientV9))
  }

}
