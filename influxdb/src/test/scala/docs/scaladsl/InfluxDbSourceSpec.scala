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
import pekko.stream.connectors.influxdb.InfluxDbReadSettings
import pekko.stream.connectors.influxdb.scaladsl.InfluxDbSource
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Sink
import pekko.testkit.TestKit
import org.influxdb.{ InfluxDB, InfluxDBException }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.concurrent.ScalaFutures
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import docs.javadsl.TestUtils._
import org.influxdb.dto.Query
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InfluxDbSourceSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  final val DatabaseName = "InfluxDbSourceSpec"

  implicit val system: ActorSystem = ActorSystem()

  implicit var influxDB: InfluxDB = _

  override protected def beforeAll(): Unit =
    influxDB = setupConnection(DatabaseName)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override def beforeEach(): Unit =
    populateDatabase(influxDB, classOf[InfluxDbSourceCpu])

  override def afterEach() =
    cleanDatabase(influxDB, DatabaseName)

  "support source" in assertAllStagesStopped {
    // #run-typed
    val query = new Query("SELECT * FROM cpu", DatabaseName)

    val influxDBResult = InfluxDbSource(influxDB, query).runWith(Sink.seq)
    val resultToAssert = influxDBResult.futureValue.head

    val values = resultToAssert.getResults.get(0).getSeries().get(0).getValues

    values.size() mustBe 2
  }

  "exception on source" in assertAllStagesStopped {
    val query = new Query("SELECT man() FROM invalid", DatabaseName)

    val result = InfluxDbSource(influxDB, query) // .runWith(Sink.seq)
      .recover {
        case e: InfluxDBException => e.getMessage
      }
      .runWith(Sink.seq)
      .futureValue

    result mustBe Seq("undefined function man()")
  }

  "partial error in query" in assertAllStagesStopped {
    val query = new Query("SELECT*FROM cpu; SELECT man() FROM invalid", DatabaseName)

    val influxDBResult = InfluxDbSource(influxDB, query).runWith(Sink.seq)
    val resultToAssert = influxDBResult.futureValue.head

    val valuesFetched = resultToAssert.getResults.get(0).getSeries().get(0).getValues
    valuesFetched.size() mustBe 2

    val error = resultToAssert.getResults.get(1).getError
    error mustBe "undefined function man()"
  }

  "exception on typed source" in assertAllStagesStopped {
    val query = new Query("SELECT man() FROM invalid", DatabaseName)

    val result = InfluxDbSource
      .typed(classOf[InfluxDbSourceCpu], InfluxDbReadSettings.Default, influxDB, query) // .runWith(Sink.seq)
      .recover {
        case e: InfluxDBException => e.getMessage
      }
      .runWith(Sink.seq)
      .futureValue

    result mustBe Seq("undefined function man()")
  }

  "mixed exception on typed source" in assertAllStagesStopped {
    val query = new Query("SELECT*FROM cpu;SELECT man() FROM invalid; SELECT*FROM cpu;", DatabaseName)

    val result = InfluxDbSource
      .typed(classOf[InfluxDbSourceCpu], InfluxDbReadSettings.Default, influxDB, query) // .runWith(Sink.seq)
      .recover {
        case e: InfluxDBException => e.getMessage
      }
      .runWith(Sink.seq)
      .futureValue

    val firstResult = result(0).asInstanceOf[InfluxDbSourceCpu]
    firstResult.getHostname mustBe "local_1"

    val error = result(1).asInstanceOf[String]
    error mustBe "not executed"
  }

}
