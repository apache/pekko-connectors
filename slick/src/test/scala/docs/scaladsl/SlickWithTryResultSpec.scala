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
import pekko.Done
import pekko.actor.ActorSystem
import pekko.stream.connectors.slick.scaladsl.{ Slick, SlickSession, SlickWithTryResult }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl._
import pekko.testkit.TestKit

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import slick.dbio.DBIOAction
import slick.jdbc.GetResult

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

class SlickWithTryResultSpec extends AnyWordSpec
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with LogCapturing {
  // #init-mat
  implicit val system: ActorSystem = ActorSystem()
  // #init-mat

  // #init-session
  implicit val session: SlickSession = SlickSession.forConfig("slick-h2")
  // #init-session

  import session.profile.api._

  case class User(id: Int, name: String)
  class Users(tag: Tag) extends Table[(Int, String)](tag, "PEKKO_CONNECTORS_SLICK_SCALADSL_TEST_USERS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def * = (id, name)
  }

  implicit val ec: ExecutionContext = system.dispatcher
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 50.millis)
  implicit val getUserResult: GetResult[User] = GetResult(r => User(r.nextInt(), r.nextString()))

  val users = (1 to 40).map(i => User(i, s"Name$i")).toSet
  val duplicateUser = scala.collection.immutable.Seq(users.head, users.head)

  val createTable =
    sqlu"""CREATE TABLE PEKKO_CONNECTORS_SLICK_SCALADSL_TEST_USERS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))"""
  val dropTable = sqlu"""DROP TABLE PEKKO_CONNECTORS_SLICK_SCALADSL_TEST_USERS"""
  val selectAllUsers = sql"SELECT ID, NAME FROM PEKKO_CONNECTORS_SLICK_SCALADSL_TEST_USERS".as[User]
  val typedSelectAllUsers = TableQuery[Users].result

  def insertUser(user: User): DBIO[Int] =
    sqlu"INSERT INTO PEKKO_CONNECTORS_SLICK_SCALADSL_TEST_USERS VALUES(${user.id}, ${user.name})"

  def getAllUsersFromDb: Future[Set[User]] = Slick.source(selectAllUsers).runWith(Sink.seq).map(_.toSet)
  def populate(): Unit = {
    val actions = users.map(insertUser)

    // This uses the standard Slick API exposed by the Slick session
    // on purpose, just to double-check that inserting data through
    // our Pekko connectors is equivalent to inserting it the Slick way.
    session.db.run(DBIO.seq(actions.toList: _*)).futureValue
  }

  override def beforeEach(): Unit = session.db.run(createTable).futureValue
  override def afterEach(): Unit = session.db.run(dropTable).futureValue

  override def afterAll(): Unit = {
    // #close-session
    system.registerOnTermination(() => session.close())
    // #close-session

    TestKit.shutdownActorSystem(system)
  }

  "SlickSession.forDbAndProfile" must {
    "create a slick session able to talk to the db" in {
      // #init-session-from-db-and-profile
      val db = Database.forConfig("slick-h2.db")
      val profile = slick.jdbc.H2Profile
      val slickSessionCreatedForDbAndProfile: SlickSession = SlickSession.forDbAndProfile(db, profile)
      // #init-session-from-db-and-profile
      try {
        val q = sql"select true".as[Boolean]
        val result = Slick
          .source(q)(slickSessionCreatedForDbAndProfile)
          .runWith(Sink.head)
          .futureValue
        assert(result === true)
      } finally {
        slickSessionCreatedForDbAndProfile.close()
      }
    }
  }

  "SlickWithTryResult.flowTry(..)" must {
    "insert 40 records into a table (no parallelism)" in {
      val inserted = Source(users)
        .via(SlickWithTryResult.flowTry(insertUser))
        .runWith(Sink.seq)
        .futureValue

      inserted must have size users.size
      inserted.toSet mustBe Set(1)

      getAllUsersFromDb.futureValue mustBe users
    }

    "insert 40 records into a table (parallelism = 4)" in {
      val inserted = Source(users)
        .via(SlickWithTryResult.flowTry(parallelism = 4, insertUser))
        .runWith(Sink.seq)
        .futureValue

      inserted must have size users.size

      getAllUsersFromDb.futureValue mustBe users
    }

    "insert 40 records into a table faster using Flow.grouped (n = 10, parallelism = 4)" in {
      val inserted = Source(users)
        .grouped(10)
        .via(
          SlickWithTryResult.flowTry(parallelism = 4,
            (group: Seq[User]) => group.map(insertUser).reduceLeft(_.andThen(_))))
        .runWith(Sink.seq)
        .futureValue

      inserted must have size 4

      getAllUsersFromDb.futureValue mustBe users
    }
  }

  "SlickWithTryResult.flowTry(..)" must {
    "insert 40 records into a table with try (no parallelism)" in {
      val inserted = Source(users)
        .via(SlickWithTryResult.flowTry(insertUser))
        .runWith(Sink.last)
        .futureValue

      inserted mustBe Success(1)

      getAllUsersFromDb.futureValue mustBe users
    }

    "insert 40 records into a table with try (parallelism = 4)" in {
      val inserted = Source(users)
        .via(SlickWithTryResult.flowTry(parallelism = 4, insertUser))
        .runWith(Sink.last)
        .futureValue

      inserted mustBe Success(1)

      getAllUsersFromDb.futureValue mustBe users
    }

    "insert 40 records into a table with try faster using Flow.grouped (n = 10, parallelism = 4)" in {
      val inserted = Source(users)
        .grouped(10)
        .via(
          SlickWithTryResult.flowTry(parallelism = 4,
            (group: Seq[User]) => group.map(insertUser).reduceLeft(_.andThen(_))))
        .runWith(Sink.seq)
        .futureValue

      inserted must have size 4

      getAllUsersFromDb.futureValue mustBe users
    }
  }

  "SlickWithTryResult.flowTryWithPassThrough(..)" must {
    "inserting 40 records into a table with try (no parallelism)" in {
      val inserted = Source(users)
        .via(SlickWithTryResult.flowTryWithPassThrough { user =>
          insertUser(user).map(insertCount => (user, insertCount))
        })
        .runWith(Sink.seq)
        .futureValue

      inserted must have size (users.size)
      inserted.collect { case Success((u, _)) => u }.toSet mustBe users
      inserted.collect { case Success((_, i)) => i }.toSet mustBe Set(1)

      getAllUsersFromDb.futureValue mustBe users
    }

    "inserting 40 records into a table with try (parallelism = 4)" in {
      val inserted = Source(users)
        .via(SlickWithTryResult.flowTryWithPassThrough(parallelism = 4,
          user => {
            insertUser(user).map(insertCount => (user, insertCount))
          }))
        .runWith(Sink.seq)
        .futureValue

      inserted must have size users.size

      getAllUsersFromDb.futureValue mustBe users
    }

    "inserting 40 records into a table with try faster using Flow.grouped (n = 10, parallelism = 4)" in {
      val inserted = Source(users)
        .grouped(10)
        .via(
          SlickWithTryResult.flowTryWithPassThrough(
            parallelism = 4,
            (group: Seq[User]) => {
              val groupedDbActions = group.map(user => insertUser(user).map(insertCount => Seq((user, insertCount))))
              DBIOAction.fold(groupedDbActions, Seq.empty[(User, Int)])(_ ++ _)
            }))
        .collect { case Success(r) => r }
        .runWith(Sink.fold(Seq.empty[(User, Int)])((a, b) => a ++ b))
        .futureValue

      inserted must have size users.size
      inserted.map(_._1).toSet mustBe users
      inserted.map(_._2).toSet mustBe Set(1)

      getAllUsersFromDb.futureValue mustBe users
    }

    "not throw an exception, but return `[Failure]` when there is any from the db" in {
      val inserted = Source(duplicateUser)
        .via(SlickWithTryResult.flowTryWithPassThrough { user =>
          insertUser(user).map(insertCount => (user, insertCount))
        })
        .runWith(Sink.last)
        .futureValue

      inserted mustBe a[Failure[_]]

      getAllUsersFromDb.futureValue mustBe Set(users.head)
    }

    "kafka-example - try store documents and pass Responses with passThrough if successful" in {

      // #kafka-example
      // We're going to pretend we got messages from kafka.
      // After we've written them to a db with Slick, we want
      // to commit the offset to Kafka

      case class KafkaOffset(offset: Int)
      case class KafkaMessage[A](msg: A, offset: KafkaOffset) {
        // map the msg and keep the offset
        def map[B](f: A => B): KafkaMessage[B] = KafkaMessage(f(msg), offset)
      }

      val messagesFromKafka = users.zipWithIndex.map { case (user, index) => KafkaMessage(user, KafkaOffset(index)) }

      var committedOffsets = List[KafkaOffset]()

      def commitToKafka(offset: KafkaOffset): Future[Done] = {
        committedOffsets = committedOffsets :+ offset
        Future.successful(Done)
      }

      val f1 = Source(messagesFromKafka) // Assume we get this from Kafka
        .via( // write to db with Slick
          SlickWithTryResult.flowTryWithPassThrough { kafkaMessage =>
            insertUser(kafkaMessage.msg).map(insertCount => kafkaMessage.map(_ => insertCount))
          })
        .collect { case Success(x) => x }
        .mapAsync(1) { kafkaMessage =>
          if (kafkaMessage.msg == 0) throw new Exception("Failed to write message to db")
          // Commit to kafka
          commitToKafka(kafkaMessage.offset)
        }
        .runWith(Sink.seq)

      Await.ready(f1, Duration.Inf)

      // Make sure all messages were committed to kafka
      committedOffsets.map(_.offset).sorted mustBe (0 until users.size).toList

      // Assert that all docs were written to db
      getAllUsersFromDb.futureValue mustBe users
    }
  }

  "SlickWithTryResult.sinkTry(..)" must {
    "insert 40 records into a table (no parallelism)" in {
      val inserted = Source(users)
        .runWith(SlickWithTryResult.sinkTry(insertUser))
        .futureValue

      inserted mustBe Success(1)
      getAllUsersFromDb.futureValue mustBe users
    }

    "insert 40 records into a table (parallelism = 4)" in {
      val inserted = Source(users)
        .runWith(SlickWithTryResult.sinkTry(parallelism = 4, insertUser))
        .futureValue

      inserted mustBe Success(1)
      getAllUsersFromDb.futureValue mustBe users
    }

    "insert 40 records into a table faster using Flow.grouped (n = 10, parallelism = 4)" in {
      val inserted = Source(users)
        .grouped(10)
        .runWith(
          SlickWithTryResult.sinkTry(parallelism = 4,
            (group: Seq[User]) => group.map(insertUser).reduceLeft(_.andThen(_))))
        .futureValue

      inserted mustBe Success(1)
      getAllUsersFromDb.futureValue mustBe users
    }

    "produce Failure(_) when inserting duplicate record" in {
      val inserted = Source(duplicateUser)
        .runWith(SlickWithTryResult.sinkTry(insertUser))
        .futureValue

      inserted mustBe a[Failure[_]]
      getAllUsersFromDb.futureValue mustBe Set(users.head)
    }

    "produce `Failure[_]` when inserting duplicate record (parallelism = 4)" in {
      val records = users.toSeq :+ users.head

      val inserted = Source(records)
        .runWith(SlickWithTryResult.sinkTry(parallelism = 4, insertUser))
        .futureValue

      records.length mustBe 41
      inserted mustBe a[Failure[_]]
      getAllUsersFromDb.futureValue mustBe users
    }

  }
}
