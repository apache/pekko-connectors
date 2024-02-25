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

import com.couchbase.client.core.error.{
  DocumentNotFoundException,
  DurabilityAmbiguousException,
  ReplicaNotConfiguredException
}
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.{ GetOptions, GetResult, MutationResult }
import org.apache.pekko
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import pekko.Done
import pekko.stream.connectors.couchbase._
import pekko.stream.connectors.couchbase.scaladsl.CouchbaseFlow
import pekko.stream.connectors.couchbase.testing.{ CouchbaseSupport, TestObject }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Sink, Source }
import scala.jdk.FutureConverters.CompletionStageOps

//#write-settings
import com.couchbase.client.java.kv.{ PersistTo, ReplicateTo }
//#write-settings

import org.apache.pekko.stream.connectors.couchbase.testing.StringDocument
import pekko.stream.testkit.scaladsl.StreamTestKit._
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future

//#init-sourceBulk
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

//#init-sourceBulk

class CouchbaseFlowSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CouchbaseSupport
    with Matchers
    with ScalaFutures
    with Inspectors
    with LogCapturing {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 250.millis)

  override def beforeAll(): Unit = super.beforeAll()

  override def afterAll(): Unit = super.afterAll()

  "Couchbase settings" should {

    "create default writeSettings object" in assertAllStagesStopped {

      val writeSettings = CouchbaseWriteSettings()
      val expectedWriteSetting = CouchbaseWriteSettings(1, ReplicateTo.NONE, PersistTo.NONE, 2.seconds)
      writeSettings shouldEqual expectedWriteSetting
    }

    "create custom writeSettings object" in {

      // #write-settings
      val writeSettings = CouchbaseWriteSettings()
        .withParallelism(3)
        .withPersistTo(PersistTo.FOUR)
        .withReplicateTo(ReplicateTo.THREE)
        .withTimeout(5.seconds)
      // #write-settings

      val expectedWriteSettings = CouchbaseWriteSettings(3, ReplicateTo.THREE, PersistTo.FOUR, 5.seconds)
      writeSettings shouldEqual expectedWriteSettings
    }
  }

  "Couchbase upsert" should {

    "insert RawJsonDocument" in assertAllStagesStopped {
      val result: Future[Done] =
        Source
          .single(sampleData)
          .map(toRawJsonDocument)
          .via(
            CouchbaseFlow.upsert(
              sessionSettings,
              writeSettings,
              bucketName,
              _.id))
          .runWith(Sink.ignore)
      result.futureValue
      val msgFuture: Future[GetResult] = session.collection(bucketName)
        .get(sampleData.id, GetOptions.getOptions)
        .asScala
      msgFuture.futureValue.contentAs(classOf[StringDocument]).id shouldEqual sampleData.id

    }

    "insert JsonDocument" in assertAllStagesStopped {

      // #upsert
      val obj = TestObject(id = "First", "First")

      val writeSettings = CouchbaseWriteSettings()

      val jsonDocumentUpsert: Future[Done] =
        Source
          .single(obj)
          .map(toJsonDocument)
          .via(
            CouchbaseFlow.upsert(
              sessionSettings,
              writeSettings,
              bucketName,
              _.getString("id")))
          .runWith(Sink.ignore)
      // #upsert
      jsonDocumentUpsert.futureValue

      val msgFuture: Future[JsonObject] = session.getJson(session.collection(bucketName), obj.id)
      msgFuture.futureValue.getString("id") shouldEqual sampleData.id
    }

    "insert StringDocument" in assertAllStagesStopped {
      // #upsert

      val stringDocumentUpsert: Future[Done] =
        Source
          .single(sampleData)
          .map(toStringDocument)
          .via(
            CouchbaseFlow.upsert(
              sessionSettings,
              writeSettings,
              bucketName,
              _.id))
          .runWith(Sink.ignore)
      // #upsert
      stringDocumentUpsert.futureValue

      val msgFuture: Future[StringDocument] =
        session.get(session.collection(bucketName), sampleData.id, classOf[StringDocument])
      msgFuture.futureValue.id shouldEqual sampleData.id
    }

    "insert BinaryDocument" in assertAllStagesStopped {
      val result: Future[Done] =
        Source
          .single(sampleData)
          .map(toBinaryDocument)
          .via(CouchbaseFlow.upsert(sessionSettings, writeSettings, bucketName, _.id))
          .runWith(Sink.ignore)
      result.futureValue

      val msgFuture: Future[JsonObject] =
        session.getJson(session.collection(bucketName), sampleData.id)
      msgFuture.futureValue.getString("id") shouldEqual sampleData.id
    }

    "insert multiple RawJsonDocuments" in assertAllStagesStopped {
      val bulkUpsertResult: Future[Done] = Source(sampleSequence)
        .map(toRawJsonDocument)
        .via(
          CouchbaseFlow.upsert(
            sessionSettings,
            writeSettings.withParallelism(2),
            bucketName,
            _.id))
        .runWith(Sink.ignore)

      bulkUpsertResult.futureValue

      val resultsAsFuture: Future[immutable.Seq[StringDocument]] =
        Source(sampleSequence.map(_.id))
          .via(CouchbaseFlow.fromId(sessionSettings, bucketName, classOf[StringDocument]))
          .runWith(Sink.seq)

      resultsAsFuture.futureValue.map(_.id) should contain.inOrderOnly("First", "Second", "Third", "Fourth")
    }

    "insert multiple JsonDocuments" in assertAllStagesStopped {
      val bulkUpsertResult: Future[Done] = Source(sampleSequence)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.upsert(sessionSettings, writeSettings.withParallelism(2), bucketName, _.getString("id")))
        .runWith(Sink.ignore)

      bulkUpsertResult.futureValue

      // #fromId
      val ids = immutable.Seq("First", "Second", "Third", "Fourth")

      val futureResult: Future[immutable.Seq[JsonObject]] =
        Source(ids)
          .via(
            CouchbaseFlow.fromId(
              sessionSettings,
              bucketName))
          .map(_.contentAsObject())
          .runWith(Sink.seq)
      // #fromId

      futureResult.futureValue.map(_.getString("id")) should contain.inOrderOnly("First", "Second", "Third", "Fourth")
    }

    "insert multiple StringDocuments" in assertAllStagesStopped {
      val bulkUpsertResult: Future[Done] = Source(sampleSequence)
        .map(toStringDocument)
        .via(
          CouchbaseFlow.upsert(
            sessionSettings,
            writeSettings.withParallelism(2),
            bucketName,
            _.id))
        .runWith(Sink.ignore)
      bulkUpsertResult.futureValue

      val resultsAsFuture: Future[immutable.Seq[StringDocument]] =
        Source(sampleSequence.map(_.id))
          .via(
            CouchbaseFlow.fromId(
              sessionSettings,
              bucketName,
              classOf[StringDocument]))
          .runWith(Sink.seq)

      resultsAsFuture.futureValue.map(_.id) should contain.inOrder("First", "Second", "Third", "Fourth")
    }

    "insert multiple BinaryDocuments" in assertAllStagesStopped {
      val bulkUpsertResult: Future[Done] = Source(sampleSequence)
        .map(toBinaryDocument)
        .via(
          CouchbaseFlow.upsert(
            sessionSettings,
            writeSettings.withParallelism(2),
            bucketName,
            _.id))
        .runWith(Sink.ignore)
      bulkUpsertResult.futureValue

      val resultsAsFuture: Future[immutable.Seq[JsonObject]] =
        Source(sampleSequence.map(_.id))
          .via(
            CouchbaseFlow.fromId(
              sessionSettings,
              bucketName))
          .map(_.contentAsObject())
          .runWith(Sink.seq)
      resultsAsFuture.futureValue.map(_.getString("id")) shouldBe Seq("First", "Second", "Third", "Fourth")
    }

    "fails stream when ReplicateTo higher then #of nodes" in assertAllStagesStopped {
      val bulkUpsertResult = Source(sampleSequence)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.upsert(sessionSettings,
            writeSettings
              .withParallelism(2)
              .withPersistTo(PersistTo.THREE)
              .withTimeout(1.seconds),
            bucketName,
            _.getString("id")))
        .runWith(Sink.head)
      // @see com.couchbase.client.core.service.kv.Observe.validateReplicas
      bulkUpsertResult.failed.futureValue.getCause shouldBe a[ReplicaNotConfiguredException]
    }
  }

  "Couchbase delete" should {

    "delete single element" in assertAllStagesStopped {
      val upsertFuture: Future[Done] =
        Source
          .single(sampleData)
          .map(toRawJsonDocument)
          .via(
            CouchbaseFlow.upsert(
              sessionSettings,
              writeSettings,
              bucketName,
              _.id))
          .runWith(Sink.ignore)
      // wait til operation completed
      upsertFuture.futureValue

      // #delete
      val deleteFuture: Future[Done] =
        Source
          .single(sampleData.id)
          .via(
            CouchbaseFlow.delete(
              sessionSettings,
              writeSettings,
              bucketName))
          .runWith(Sink.ignore)
      // #delete
      deleteFuture.futureValue

      Thread.sleep(1000)

      val getFuture: Future[StringDocument] =
        Source
          .single(sampleData.id)
          .via(
            CouchbaseFlow
              .fromId(
                sessionSettings,
                bucketName,
                classOf[StringDocument]))
          .runWith(Sink.head)
      // DocumentNotFoundException
      getFuture.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]
    }

    "delete elements and some do not exist" in assertAllStagesStopped {
      val bulkUpsertResult: Future[Done] = Source(sampleSequence)
        .map(toRawJsonDocument)
        .via(
          CouchbaseFlow.upsert(sessionSettings, writeSettings.withParallelism(2), bucketName, _.id))
        .runWith(Sink.ignore)
      bulkUpsertResult.futureValue

      val deleteFuture: Future[Done] = Source(sampleSequence.map(_.id) :+ "NoneExisting")
        .via(
          CouchbaseFlow.delete(sessionSettings, writeSettings.withParallelism(2), bucketName))
        .runWith(Sink.ignore)
      deleteFuture.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]

      val getFuture: Future[Seq[StringDocument]] =
        Source(sampleSequence.map(_.id))
          .via(
            CouchbaseFlow.fromId(sessionSettings, bucketName, classOf[StringDocument]))
          .runWith(Sink.seq)
      getFuture.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]
    }
  }

  "Couchbase get" should {

    "get document in flow" in assertAllStagesStopped {
      upsertSampleData(queryBucketName)

      val id = "First"

      val result: Future[JsonObject] = Source
        .single(id)
        .via(
          CouchbaseFlow.fromId(sessionSettings, queryBucketName))
        .map(_.contentAsObject())
        .runWith(Sink.head)
      result.futureValue.getString("id") shouldEqual id
    }

    "get document in flow that does not exist" in assertAllStagesStopped {
      val id = "not exists"

      val result: Future[JsonObject] = Source
        .single(id)
        .via(CouchbaseFlow.fromId(sessionSettings, bucketName))
        .map(_.contentAsObject())
        .runWith(Sink.head)
      result.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]
    }

    "get bulk of documents as part of the flow" in assertAllStagesStopped {
      upsertSampleData(queryBucketName)

      val result: Future[Seq[JsonObject]] = Source(sampleSequence.map(_.id))
        .via(CouchbaseFlow.fromId(sessionSettings, queryBucketName))
        .map(_.contentAsObject())
        .runWith(Sink.seq)
      result.futureValue.map(_.getString("id")) shouldBe Seq("First", "Second", "Third", "Fourth")
    }

    "get bulk of documents as part of the flow where not all ids exist" in assertAllStagesStopped {
      upsertSampleData(queryBucketName)

      val result: Future[Seq[JsonObject]] = Source
        .apply(sampleSequence.map(_.id) :+ "Not Existing Id")
        .via(CouchbaseFlow.fromId(sessionSettings, queryBucketName))
        .map(_.contentAsObject())
        .runWith(Sink.seq)
      result.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]
    }
  }

  "Couchbase replace" should {

    "replace single element" in assertAllStagesStopped {
      upsertSampleData(bucketName)

      val obj = TestObject("Second", "SecondReplace")
      // #replace
      val replaceFuture: Future[MutationResult] =
        Source
          .single(obj)
          .map(toStringDocument)
          .via(CouchbaseFlow.replace(sessionSettings, writeSettings, bucketName, _.id))
          .runWith(Sink.head)
      // #replace
      replaceFuture.futureValue
      Thread.sleep(1000)
      val getFuture = session.collection(bucketName).get(obj.id).asScala
      getFuture.futureValue.contentAsObject().getString("content") shouldEqual (toStringDocument(obj).content)

    }

    "replace multiple StringDocuments" in assertAllStagesStopped {

      val replaceSequence: Seq[TestObject] = sampleData +: Seq[TestObject](TestObject("Second", "SecondReplace"),
        TestObject("Third", "ThirdReplace"))

      upsertSampleData(bucketName)

      val bulkReplaceResult: Future[Done] = Source(replaceSequence)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.replace(sessionSettings, writeSettings.withParallelism(2), bucketName, _.getString("id")))
        .runWith(Sink.ignore)

      bulkReplaceResult.futureValue

      val resultsAsFuture: Future[immutable.Seq[JsonObject]] =
        Source(sampleSequence.map(_.id))
          .via(CouchbaseFlow.fromId(sessionSettings, bucketName))
          .map(_.contentAsObject())
          .runWith(Sink.seq)

      resultsAsFuture.futureValue.map(_.getString("value")) should contain.inOrderOnly("First",
        "SecondReplace", "ThirdReplace", "Fourth")
    }

    "replace RawJsonDocument" in assertAllStagesStopped {

      upsertSampleData(bucketName)

      val obj = TestObject("Second", "SecondReplace")

      // #replaceDoc
      val replaceDocFuture: Future[Done] = Source
        .single(obj)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.replaceJson(sessionSettings, writeSettings, bucketName, _.getString("id")))
        .runWith(Sink.ignore)
      // #replaceDocreplace

      replaceDocFuture.futureValue

      Thread.sleep(1000)

      val msgFuture: Future[JsonObject] = session.getJson(session.collection(bucketName), obj.id)
      msgFuture.futureValue.get("value") shouldEqual obj.value
    }

    "fails stream when ReplicateTo higher then #of nodes" in assertAllStagesStopped {

      upsertSampleData(bucketName)

      val bulkReplaceResult: Future[immutable.Seq[MutationResult]] = Source(sampleSequence)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.replaceJson(sessionSettings,
            writeSettings
              .withParallelism(2)
              .withPersistTo(PersistTo.THREE)
              .withTimeout(1.seconds), bucketName, _.getString("id")))
        .runWith(Sink.seq)
      bulkReplaceResult.failed.futureValue.getCause shouldBe a[ReplicaNotConfiguredException]
    }
  }

  "Couchbase upsert with result" should {
    "write documents" in assertAllStagesStopped {
      // #upsertDocWithResult

      val result: Future[immutable.Seq[CouchbaseWriteResult[StringDocument]]] =
        Source(sampleSequence)
          .map(toRawJsonDocument)
          .via(
            CouchbaseFlow.upsertWithResult(
              sessionSettings,
              writeSettings,
              bucketName,
              _.id))
          .runWith(Sink.seq)

      val failedDocs: immutable.Seq[CouchbaseWriteFailure[StringDocument]] = result.futureValue.collect {
        case res: CouchbaseWriteFailure[StringDocument] => res
      }
      // #upsertDocWithResult

      result.futureValue should have size sampleSequence.size
      failedDocs shouldBe empty
      forAll(result.futureValue)(_ shouldBe Symbol("success"))
    }

    "expose failures in-stream" in assertAllStagesStopped {

      val result: Future[immutable.Seq[CouchbaseWriteResult[JsonObject]]] = Source(sampleSequence)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.upsertWithResult(sessionSettings,
            writeSettings
              .withParallelism(2)
              .withPersistTo(PersistTo.THREE)
              .withTimeout(1.seconds),
            bucketName,
            _.getString("id")))
        .runWith(Sink.seq)

      result.futureValue should have size sampleSequence.size
      val failedDocs: immutable.Seq[CouchbaseWriteFailure[JsonObject]] = result.futureValue.collect {
        case res: CouchbaseWriteFailure[JsonObject] => res
      }
      failedDocs.head.failure shouldBe a[ReplicaNotConfiguredException]
    }

  }

  "delete with result" should {
    "propagate an error in-stream" in assertAllStagesStopped {
      val deleteFuture: Future[String] =
        Source
          .single("non-existent")
          .via(
            CouchbaseFlow.delete(
              sessionSettings,
              writeSettings
                .withParallelism(2)
                .withReplicateTo(ReplicateTo.THREE)
                .withTimeout(1.seconds),
              bucketName))
          .runWith(Sink.head)

      // #deleteWithResult
      val deleteResult: Future[CouchbaseDeleteResult] =
        Source
          .single("non-existent")
          .via(
            CouchbaseFlow.deleteWithResult(
              sessionSettings,
              writeSettings,
              bucketName))
          .runWith(Sink.head)
      // #deleteWithResult
      deleteFuture.failed.futureValue.getCause shouldBe a[DocumentNotFoundException]

      deleteResult.futureValue shouldBe a[CouchbaseDeleteFailure]
      deleteResult.futureValue.id shouldBe "non-existent"
      deleteResult.mapTo[CouchbaseDeleteFailure].futureValue.failure.getCause shouldBe a[DocumentNotFoundException]
    }
  }

  "replace with result" should {
    "replace documents" in assertAllStagesStopped {

      upsertSampleData(bucketName)

      // #replaceDocWithResult

      val result: Future[immutable.Seq[CouchbaseWriteResult[StringDocument]]] =
        Source(sampleSequence)
          .map(toRawJsonDocument)
          .via(
            CouchbaseFlow.replaceWithResult(
              sessionSettings,
              writeSettings,
              bucketName,
              _.id))
          .runWith(Sink.seq)

      val failedDocs: immutable.Seq[CouchbaseWriteFailure[StringDocument]] = result.futureValue.collect {
        case res: CouchbaseWriteFailure[StringDocument] => res
      }
      // #replaceDocWithResult

      result.futureValue should have size sampleSequence.size
      failedDocs shouldBe empty
      forAll(result.futureValue)(_ shouldBe Symbol("success"))
    }

    "expose failures in-stream" in assertAllStagesStopped {

      cleanAllInBucket(bucketName)

      val result: Future[immutable.Seq[CouchbaseWriteResult[JsonObject]]] = Source(sampleSequence)
        .map(toJsonDocument)
        .via(
          CouchbaseFlow.replaceWithResult(sessionSettings,
            writeSettings
              .withParallelism(2)
              .withPersistTo(PersistTo.THREE)
              .withTimeout(1.seconds),
            bucketName,
            _.getString("id")))
        .runWith(Sink.seq)

      result.futureValue should have size sampleSequence.size
      val failedDocs: immutable.Seq[CouchbaseWriteFailure[JsonObject]] = result.futureValue.collect {
        case res: CouchbaseWriteFailure[JsonObject] => res
      }
      failedDocs.head.failure.getCause shouldBe a[DocumentNotFoundException]
    }
  }
}
