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
import pekko.{ Done, NotUsed }
import pekko.actor.ActorSystem
import pekko.stream.connectors.kudu.{ KuduAttributes, KuduTableSettings }
import pekko.stream.connectors.kudu.scaladsl.KuduTable
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import pekko.testkit.TestKit
import org.apache.kudu.client.{ CreateTableOptions, KuduClient, PartialRow }
import org.apache.kudu.{ ColumnSchema, Schema, Type }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class KuduTableSpec
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  // #configure
  // Kudu Schema
  val cols = List(new ColumnSchema.ColumnSchemaBuilder("key", Type.INT32).key(true).build,
    new ColumnSchema.ColumnSchemaBuilder("value", Type.STRING).build)
  val schema = new Schema(cols.asJava)

  // Converter function
  case class Person(id: Int, name: String)
  val kuduConverter: Person => PartialRow = { person =>
    val partialRow = schema.newPartialRow()
    partialRow.addInt(0, person.id)
    partialRow.addString(1, person.name)
    partialRow
  }

  // Kudu table options
  val rangeKeys = List("key")
  val createTableOptions = new CreateTableOptions().setNumReplicas(1).setRangePartitionColumns(rangeKeys.asJava)

  // Pekko Connectors Settings
  val kuduTableSettings = KuduTableSettings("test", schema, createTableOptions, kuduConverter)
  // #configure

  "Kudu stages " must {

    "sinks in kudu" in {
      // #sink
      val sink: Sink[Person, Future[Done]] =
        KuduTable.sink(kuduTableSettings.withTableName("Sink"))

      val f = Source(1 to 10)
        .map(i => Person(i, s"zozo_$i"))
        .runWith(sink)
      // #sink

      f.futureValue should be(Done)
    }

    "flows through kudu" in {
      // #flow
      val flow: Flow[Person, Person, NotUsed] =
        KuduTable.flow(kuduTableSettings.withTableName("Flow"))

      val f = Source(11 to 20)
        .map(i => Person(i, s"zozo_$i"))
        .via(flow)
        .runWith(Sink.fold(0)((a, d) => a + d.id))
      // #flow

      f.futureValue should be(155)
    }

    "custom client" in {
      // #attributes
      val masterAddress = "localhost:7051"
      val client = new KuduClient.KuduClientBuilder(masterAddress).build
      system.registerOnTermination(client.shutdown())

      val flow: Flow[Person, Person, NotUsed] =
        KuduTable
          .flow(kuduTableSettings.withTableName("Flow"))
          .withAttributes(KuduAttributes.client(client))
      // #attributes

      val f = Source(11 to 20)
        .map(i => Person(i, s"zozo_$i"))
        .via(flow)
        .runWith(Sink.fold(0)((a, d) => a + d.id))

      f.futureValue should be(155)
    }

  }

}
