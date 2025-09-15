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
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.stream.connectors.mongodb.scaladsl.MongoSource
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.Document
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures

import scala.annotation.nowarn
import scala.collection.immutable.Seq
import scala.concurrent._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class MongoSourceSpec
    extends AnyWordSpec
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with LogCapturing {

  // #init-system
  implicit val system: ActorSystem = ActorSystem()
  // #init-system

  override protected def beforeAll(): Unit =
    Source.fromPublisher(db.drop()).runWith(Sink.headOption).futureValue

  java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(java.util.logging.Level.SEVERE)

  // #pojo
  case class Number(_id: Int)
  // #pojo

  // #codecs
  import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
  import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  import org.mongodb.scala.bson.codecs.Macros._

  val codecRegistry =
    fromRegistries(fromProviders(classOf[Number]: @nowarn("msg=match may not be exhaustive")), DEFAULT_CODEC_REGISTRY)
  // #codecs

  // #init-connection
  private val client = MongoClients.create("mongodb://localhost:27017")
  private val db = client.getDatabase("MongoSourceSpec")
  private val numbersColl = db
    .getCollection("numbers", classOf[Number])
    .withCodecRegistry(codecRegistry)
  // #init-connection

  private val numbersDocumentColl = db.getCollection("numbers")

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.millis)

  override def afterEach(): Unit =
    Source.fromPublisher(numbersDocumentColl.deleteMany(new Document())).runWith(Sink.head).futureValue

  override def afterAll(): Unit =
    system.terminate().futureValue

  private def seed() = {
    val numbers = 1 until 10
    Source
      .fromPublisher(numbersDocumentColl.insertMany {
        numbers.map { number =>
          Document.parse(s"{_id:$number}")
        }.asJava
      })
      .runWith(Sink.head)
      .futureValue
    numbers
  }

  "MongoSourceSpec" must {

    "stream the result of a simple Mongo query" in assertAllStagesStopped {
      val data: Seq[Int] = seed()

      val source: Source[Document, NotUsed] =
        MongoSource(numbersDocumentColl.find())

      val rows: Future[Seq[Document]] = source.runWith(Sink.seq)

      rows.futureValue.map(_.getInteger("_id")) must contain theSameElementsAs data
    }

    "support codec registry to read case class objects" in assertAllStagesStopped {
      val data: Seq[Number] = seed().map(Number)

      // #create-source
      val source: Source[Number, NotUsed] =
        MongoSource(numbersColl.find(classOf[Number]))
      // #create-source

      // #run-source
      val rows: Future[Seq[Number]] = source.runWith(Sink.seq)
      // #run-source

      rows.futureValue must contain theSameElementsAs data
    }

    "support multiple materializations" in assertAllStagesStopped {
      val data: Seq[Int] = seed()
      val numbersObservable = numbersDocumentColl.find()

      val source = MongoSource(numbersObservable)

      source.runWith(Sink.seq).futureValue.map(_.getInteger("_id")) must contain theSameElementsAs data
      source.runWith(Sink.seq).futureValue.map(_.getInteger("_id")) must contain theSameElementsAs data
    }

    "stream the result of Mongo query that results in no data" in assertAllStagesStopped {
      val numbersObservable = numbersDocumentColl.find()

      val rows = MongoSource(numbersObservable).runWith(Sink.seq).futureValue

      rows mustBe empty
    }
  }
}
