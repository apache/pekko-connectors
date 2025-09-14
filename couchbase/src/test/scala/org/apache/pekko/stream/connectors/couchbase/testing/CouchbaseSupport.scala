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

package org.apache.pekko.stream.connectors.couchbase.testing

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.stream.connectors.couchbase.scaladsl.{ CouchbaseFlow, CouchbaseSession }
import pekko.stream.connectors.couchbase.{ CouchbaseSessionSettings, CouchbaseWriteSettings }
import pekko.stream.scaladsl.{ Sink, Source }
import com.couchbase.client.deps.io.netty.buffer.Unpooled
import com.couchbase.client.deps.io.netty.util.CharsetUtil
import com.couchbase.client.java.ReplicateTo
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.document.{ BinaryDocument, JsonDocument, RawJsonDocument, StringDocument }
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.jdk.CollectionConverters._

case class TestObject(id: String, value: String)

private[couchbase] object CouchbaseSupport {
  val jacksonMapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .build()
}

trait CouchbaseSupport {

  private val log = LoggerFactory.getLogger(classOf[CouchbaseSupport])

  // #init-actor-system
  implicit val actorSystem: ActorSystem = ActorSystem()
  // #init-actor-system

  val sampleData = TestObject("First", "First")

  val sampleSequence: Seq[TestObject] = sampleData +: Seq[TestObject](TestObject("Second", "Second"),
    TestObject("Third", "Third"),
    TestObject("Fourth", "Fourth"))

  val sampleJavaList: java.util.List[TestObject] = sampleSequence.asJava

  val sessionSettings = CouchbaseSessionSettings(actorSystem)
  val writeSettings: CouchbaseWriteSettings = CouchbaseWriteSettings().withReplicateTo(ReplicateTo.NONE)
  val bucketName = "pekko"
  val queryBucketName = "pekkoquery"

  var session: CouchbaseSession = _

  def beforeAll(): Unit = {
    session = Await.result(CouchbaseSession(sessionSettings, bucketName), 10.seconds)
    log.info("Done Creating CB Server")
  }

  def toRawJsonDocument(testObject: TestObject): RawJsonDocument = {
    val json = CouchbaseSupport.jacksonMapper.writeValueAsString(testObject)
    RawJsonDocument.create(testObject.id, json)
  }

  def toJsonDocument(testObject: TestObject): JsonDocument =
    JsonDocument.create(testObject.id, JsonObject.create().put("id", testObject.id).put("value", testObject.value))

  def toStringDocument(testObject: TestObject): StringDocument = {
    val json = CouchbaseSupport.jacksonMapper.writeValueAsString(testObject)
    StringDocument.create(testObject.id, json)
  }

  def toBinaryDocument(testObject: TestObject): BinaryDocument = {
    val json = CouchbaseSupport.jacksonMapper.writeValueAsString(testObject)
    val toWrite = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
    BinaryDocument.create(testObject.id, toWrite)
  }

  def upsertSampleData(bucketName: String): Unit = {
    val bulkUpsertResult: Future[Done] = Source(sampleSequence)
      .map(toJsonDocument)
      .via(CouchbaseFlow.upsert(sessionSettings, CouchbaseWriteSettings.inMemory, bucketName))
      .runWith(Sink.ignore)
    Await.result(bulkUpsertResult, 5.seconds)
    // all queries are Eventual Consistent, se we need to wait for index refresh!!
    Thread.sleep(2000)
  }

  def cleanAllInBucket(bucketName: String): Unit =
    cleanAllInBucket(sampleSequence.map(_.id), bucketName)

  def cleanAllInBucket(ids: Seq[String], bucketName: String): Unit = {
    val result: Future[Done] =
      Source(ids)
        .via(CouchbaseFlow.deleteWithResult(sessionSettings, CouchbaseWriteSettings.inMemory, bucketName))
        .runWith(Sink.ignore)
    Await.result(result, 5.seconds)
    ()
  }

  def afterAll(): Unit =
    actorSystem.terminate()
}

final class CouchbaseSupportClass extends CouchbaseSupport
