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

import org.influxdb.{ InfluxDB, InfluxDBFactory }
import org.influxdb.dto.{ Point, Query, QueryResult }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.concurrent.ScalaFutures
import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.apache.pekko.stream.connectors.influxdb.{ InfluxDbReadSettings, InfluxDbWriteMessage }
import org.apache.pekko.stream.connectors.influxdb.scaladsl.{ InfluxDbSink, InfluxDbSource }
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.testkit.TestKit
import org.apache.pekko.stream.scaladsl.Sink

import scala.jdk.CollectionConverters._
import docs.javadsl.TestUtils._
import docs.javadsl.TestConstants.{ INFLUXDB_URL, PASSWORD, USERNAME }
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InfluxDbSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  implicit val system = ActorSystem()

  final val DatabaseName = this.getClass.getSimpleName

  implicit var influxDB: InfluxDB = _

  // #define-class
  override protected def beforeAll(): Unit = {
    // #init-client
    influxDB = InfluxDBFactory.connect(INFLUXDB_URL, USERNAME, PASSWORD);
    influxDB.setDatabase(DatabaseName);
    influxDB.query(new Query("CREATE DATABASE " + DatabaseName, DatabaseName));
    // #init-client
  }

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override def beforeEach(): Unit =
    populateDatabase(influxDB, classOf[InfluxDbSpecCpu])

  override def afterEach() =
    cleanDatabase(influxDB, DatabaseName)

  "support typed source" in assertAllStagesStopped {
    val query = new Query("SELECT * FROM cpu", DatabaseName);
    val measurements =
      InfluxDbSource.typed(classOf[InfluxDbSpecCpu], InfluxDbReadSettings(), influxDB, query).runWith(Sink.seq)

    measurements.futureValue.map(_.getHostname) mustBe List("local_1", "local_2")
  }

  "InfluxDbFlow" should {

    "consume and publish measurements using typed" in assertAllStagesStopped {
      val query = new Query("SELECT * FROM cpu", DatabaseName);

      // #run-typed
      val f1 = InfluxDbSource
        .typed(classOf[InfluxDbSpecCpu], InfluxDbReadSettings(), influxDB, query)
        .map { cpu: InfluxDbSpecCpu =>
          {
            val clonedCpu = cpu.cloneAt(cpu.getTime.plusSeconds(60000))
            List(InfluxDbWriteMessage(clonedCpu))
          }
        }
        .runWith(InfluxDbSink.typed(classOf[InfluxDbSpecCpu]))
      // #run-typed

      f1.futureValue mustBe Done

      val f2 =
        InfluxDbSource.typed(classOf[InfluxDbSpecCpu], InfluxDbReadSettings(), influxDB, query).runWith(Sink.seq)

      f2.futureValue.length mustBe 4
    }

    "consume and publish measurements" in assertAllStagesStopped {
      // #run-query-result
      val query = new Query("SELECT * FROM cpu", DatabaseName);

      val f1 = InfluxDbSource(influxDB, query)
        .map(resultToPoints)
        .runWith(InfluxDbSink.create())
      // #run-query-result

      f1.futureValue mustBe Done

      val f2 =
        InfluxDbSource.typed(classOf[InfluxDbSpecCpu], InfluxDbReadSettings(), influxDB, query).runWith(Sink.seq)

      f2.futureValue.length mustBe 4
    }

    def resultToPoints(queryResult: QueryResult): List[InfluxDbWriteMessage[Point, NotUsed]] = {
      val points = for {
        results <- queryResult.getResults.asScala
        series <- results.getSeries.asScala
        values <- series.getValues.asScala
      } yield InfluxDbWriteMessage(resultToPoint(series, values))
      points.toList
    }

  }

}
