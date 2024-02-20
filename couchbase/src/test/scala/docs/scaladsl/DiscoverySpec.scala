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

import com.couchbase.client.core.error.ConfigException
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.couchbase.scaladsl.{CouchbaseSession, DiscoverySupport}
import pekko.stream.connectors.couchbase.{CouchbaseSessionRegistry, CouchbaseSessionSettings}
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.stream.connectors.couchbase.testing.JsonDocument
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DiscoverySpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures with LogCapturing {

  val config: Config = ConfigFactory.parseResources("discovery.conf")

  implicit val actorSystem: ActorSystem = ActorSystem("DiscoverySpec", config)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 250.millis)

  val bucketName = "pekko"

  override def afterAll(): Unit =
    actorSystem.terminate()

  "a Couchbasesession" should {
    "be managed by the registry" in {
      // #registry

      val registry = CouchbaseSessionRegistry(actorSystem)

      val sessionSettings = CouchbaseSessionSettings(actorSystem)
        .withEnrichAsync(DiscoverySupport.nodes())
      val sessionFuture: Future[CouchbaseSession] = registry.sessionFor(sessionSettings, bucketName)
      // #registry
      sessionFuture.failed.futureValue shouldBe a[ConfigException]
    }

    "be created from settings" in {
      // #create

      implicit val ec: ExecutionContext = actorSystem.dispatcher
      val sessionSettings = CouchbaseSessionSettings(actorSystem)
        .withEnrichAsync(DiscoverySupport.nodes())
      val sessionFuture: Future[CouchbaseSession] = CouchbaseSession(sessionSettings, bucketName)
      actorSystem.registerOnTermination(sessionFuture.flatMap(_.close()))

      val documentFuture = sessionFuture.flatMap { session =>
        val id = "myId"
        session.get(id,classOf[JsonDocument])
      }
      // #create
      documentFuture.failed.futureValue shouldBe a[RuntimeException]
    }

  }
}
