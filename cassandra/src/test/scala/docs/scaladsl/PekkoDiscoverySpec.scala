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
import pekko.Done
import pekko.actor.ActorSystem
import pekko.stream.connectors.cassandra.CassandraSessionSettings
import pekko.stream.connectors.cassandra.scaladsl.{ CassandraSession, CassandraSessionRegistry, CassandraSpecBase }
import pekko.stream.scaladsl.Sink
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped

import scala.concurrent.Future

class PekkoDiscoverySpec extends CassandraSpecBase(ActorSystem("PekkoDiscoverySpec")) {

  val sessionSettings = CassandraSessionSettings("with-pekko-discovery")
  val data = 1 until 103

  override val lifecycleSession: CassandraSession = sessionRegistry.sessionFor(sessionSettings)

  "Service discovery" must {

    "connect to Cassandra" in assertAllStagesStopped {
      val session = sessionRegistry.sessionFor(sessionSettings)
      val table = createTableName()
      withSchemaMetadataDisabled {
        for {
          _ <- lifecycleSession.executeDDL(s"""
                                              |CREATE TABLE IF NOT EXISTS $table (
                                              |    id int PRIMARY KEY
                                              |);""".stripMargin)
          _ <- Future.sequence(data.map { i =>
            lifecycleSession.executeWrite(s"INSERT INTO $table(id) VALUES ($i)")
          })
        } yield Done
      }.futureValue mustBe Done
      val rows = session.select(s"SELECT * FROM $table").map(_.getInt("id")).runWith(Sink.seq).futureValue
      rows must contain theSameElementsAs data
    }

    "fail when the contact point address is invalid" in assertAllStagesStopped {
      val sessionSettings = CassandraSessionSettings("without-pekko-discovery")
      val session = sessionRegistry.sessionFor(sessionSettings)
      val result = session.select(s"SELECT * FROM fsdfsd").runWith(Sink.head)
      val exception = result.failed.futureValue
      exception mustBe a[java.util.concurrent.CompletionException]
      exception.getCause mustBe a[com.datastax.oss.driver.api.core.AllNodesFailedException]
    }

    "fail when the port is missing" in assertAllStagesStopped {
      val sessionSettings = CassandraSessionSettings("with-pekko-discovery-no-port")
      val session = sessionRegistry.sessionFor(sessionSettings)
      val result = session.select(s"SELECT * FROM fsdfsd").runWith(Sink.head)
      val exception = result.failed.futureValue
      exception mustBe a[org.apache.pekko.ConfigurationException]
    }

    "show referencing config in docs" in {
      // #discovery
      val sessionSettings = CassandraSessionSettings("example-with-pekko-discovery")
      implicit val session = CassandraSessionRegistry.get(system).sessionFor(sessionSettings)
      // #discovery
      session.close(system.dispatcher)
    }

  }
}
