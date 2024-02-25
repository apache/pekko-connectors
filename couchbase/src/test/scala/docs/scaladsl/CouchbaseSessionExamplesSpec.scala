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

import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.java.json.JsonObject
import org.apache.pekko
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pekko.stream.connectors.couchbase.{ CouchbaseSessionRegistry, CouchbaseSessionSettings }
import pekko.stream.connectors.couchbase.scaladsl.CouchbaseSession
import pekko.stream.connectors.couchbase.testing.CouchbaseSupport
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class CouchbaseSessionExamplesSpec
    extends AnyWordSpec
    with CouchbaseSupport
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 250.millis)

  override def beforeAll(): Unit = super.beforeAll()
  override def afterAll(): Unit = super.afterAll()

  "a Couchbasesession" should {
    "be managed by the registry" in {
      // #registry
      import com.couchbase.client.java.env.ClusterEnvironment

      // Pekko extension (singleton per actor system)
      val registry = CouchbaseSessionRegistry(actorSystem)

      // If connecting to more than one Couchbase cluster, the environment should be shared
      val environment: ClusterEnvironment = ClusterEnvironment.create()
      actorSystem.registerOnTermination {
        environment.shutdown()
      }

      val sessionSettings = CouchbaseSessionSettings(actorSystem)
        .withEnvironment(environment)
      val sessionFuture: Future[CouchbaseSession] = registry.sessionFor(sessionSettings)
      // #registry
      sessionFuture.futureValue shouldBe a[CouchbaseSession]
    }

    "be created from settings" in {
      // #create
      implicit val ec: ExecutionContext = actorSystem.dispatcher
      val sessionSettings = CouchbaseSessionSettings(actorSystem)
      val sessionFuture: Future[CouchbaseSession] = CouchbaseSession(sessionSettings)
      actorSystem.registerOnTermination(sessionFuture.flatMap(_.close()))
      val id = "myId"
      val documentFuture = sessionFuture.flatMap(session => session.getJson(session.collection(bucketName), id))
      documentFuture.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]
    }
  }
}
