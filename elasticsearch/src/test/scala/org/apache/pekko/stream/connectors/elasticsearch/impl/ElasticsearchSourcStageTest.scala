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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.{ Http, HttpExt }
import pekko.stream.Materializer
import pekko.stream.connectors.elasticsearch._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Keep, Source }
import pekko.stream.testkit.scaladsl.TestSink
import pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global

class ElasticsearchSourcStageTest
    extends TestKit(ActorSystem("elasticsearchSourceStagetest"))
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with LogCapturing {

  implicit val mat: Materializer = Materializer(system)
  implicit val http: HttpExt = Http()

  "ElasticsearchSourceStage" when {
    "client cannot connect to ES" should {
      "stop the stream" in {
        val downstream = Source
          .fromGraph(
            new impl.ElasticsearchSourceStage[String](
              ElasticsearchParams.V7("es-simple-flow-index"),
              Map("query" -> """{ "match_all":{}}"""),
              ElasticsearchSourceSettings(ElasticsearchConnectionSettings("http://wololo:9202")),
              (json: String) => ScrollResponse(Some(json), None)))
          .toMat(TestSink.probe)(Keep.right)
          .run()

        downstream.request(1)
        downstream.expectError()
      }
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
}
