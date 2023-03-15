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

import scala.concurrent.Await

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.event.Logging
import pekko.stream.connectors.cassandra.{ CassandraSessionSettings, CassandraWriteSettings }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import scala.concurrent.duration._

final class CassandraSessionPerformanceSpec extends CassandraSpecBase(ActorSystem("CassandraSessionPerformanceSpec")) {

  val log = Logging(system, this.getClass)

  val data = 1 to 5 * 1000 * 1000

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(2.minutes, 100.millis)

  private val dataTableName = "largerData"
  lazy val dataTable = s"$keyspaceName.$dataTableName"

  val sessionSettings: CassandraSessionSettings = CassandraSessionSettings()
  override val lifecycleSession: CassandraSession =
    sessionRegistry.sessionFor(sessionSettings)

  lazy val session: CassandraSession = sessionRegistry.sessionFor(sessionSettings)

  // only using one partition in this test
  private val partitionId = 1L
  // only using one primary key in this test
  private val id = "1"

  def insertDataTable() = {
    lifecycleSession
      .executeDDL(s"""CREATE TABLE IF NOT EXISTS $dataTable (
                     |    partition_id bigint,
                     |    id text,
                     |    seq_nr bigint,
                     |    value bigint,
                     |    PRIMARY KEY ((partition_id, id), seq_nr)
                     |);""".stripMargin)
      .flatMap { _ =>
        Source(data)
          .via {
            CassandraFlow.createBatch(
              CassandraWriteSettings.create().withMaxBatchSize(10000),
              s"INSERT INTO $dataTable(partition_id, id, value, seq_nr) VALUES (?, ?, ?, ?)",
              (d: Int, ps) => ps.bind(Long.box(partitionId), id, Long.box(d), Long.box(d)),
              (_: Int) => partitionId)(lifecycleSession)
          }
          .runWith(Sink.ignore)
      }
      .futureValue
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    insertDataTable()
  }

  "Select" must {
    "read many rows" ignore assertAllStagesStopped {
      val t0 = System.nanoTime()
      val last =
        session
          .select(s"SELECT * FROM $dataTable WHERE partition_id = ? and id = ?", Long.box(partitionId), id)
          .map(_.getLong("value"))
          .runWith(Sink.last)
      Await.result(last, 2.minutes) mustBe data.last
      println(s"Selecting ${data.size} rows took ${(System.nanoTime() - t0) / 1000 / 1000} ms")
    }
  }
}
