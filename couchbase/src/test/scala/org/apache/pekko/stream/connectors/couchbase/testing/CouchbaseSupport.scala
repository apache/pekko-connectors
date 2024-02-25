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

import com.couchbase.client.core.deps.io.netty.buffer.{ ByteBuf, Unpooled }
import com.couchbase.client.core.deps.io.netty.util.CharsetUtil
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.env.ClusterEnvironment
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.ReplicateTo
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.pekko
import org.slf4j.LoggerFactory
import pekko.Done
import pekko.actor.ActorSystem
import pekko.stream.connectors.couchbase.{ CouchbaseSessionSetting, CouchbaseWriteSettings }
import pekko.stream.connectors.couchbase.scaladsl.{ CouchbaseFlow, CouchbaseSession }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.util.ccompat.JavaConverters._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
  // couchbase use jackson, serializer for scala
  val environment = ClusterEnvironment.builder()
    .jsonSerializer(JacksonJsonSerializer.create(CouchbaseSupport.jacksonMapper))
    .build()
  val sessionSettings = CouchbaseSessionSetting(actorSystem)
    .withEnvironment(environment)
  val writeSettings: CouchbaseWriteSettings = CouchbaseWriteSettings().withReplicateTo(ReplicateTo.NONE)
  val bucketName = "pekko"
  val queryBucketName = "pekkoquery"

  var session: CouchbaseSession = _

  def beforeAll(): Unit = {
    session = Await.result(CouchbaseSession(sessionSettings), 10.seconds)
    log.info("Done Creating CB Server")
  }

  def toRawJsonDocument(testObject: TestObject): StringDocument = {
    val json = CouchbaseSupport.jacksonMapper.writeValueAsString(testObject)
    StringDocument(testObject.id, json)
  }

  def toJsonDocument(testObject: TestObject): JsonObject = {
    JsonObject.create().put("id", testObject.id).put("value", testObject.value)
  }

  def toStringDocument(testObject: TestObject): StringDocument = {
    val json = CouchbaseSupport.jacksonMapper.writeValueAsString(testObject)
    StringDocument(testObject.id, json)
  }

  def toBinaryDocument(testObject: TestObject): BinaryDocument = {

    val json = CouchbaseSupport.jacksonMapper.writeValueAsString(testObject)
    val toWrite = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
    BinaryDocument(testObject.id, toWrite)
  }

  def toJsonObject(string: String) = {
    CouchbaseSupport.jacksonMapper.reader().readTree(string)
  }

  def upsertSampleData(bucketName: String): Unit = {
    val bulkUpsertResult: Future[Done] = Source(sampleSequence)
      .map(toJsonDocument)
      .via(CouchbaseFlow.upsert(sessionSettings, CouchbaseWriteSettings.inMemory, bucketName, _.getString("id")))
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

trait Document[T] {
  def id: String
  def content: T
}

case class StringDocument(id: String, content: String)
    extends Document[String]

case class BinaryDocument(id: String, content: ByteBuf)
    extends Document[ByteBuf]
