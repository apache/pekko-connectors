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
import pekko.stream.connectors.couchbase.scaladsl.{ CouchbaseSession, CouchbaseSource }
import pekko.stream.connectors.couchbase.testing.CouchbaseSupport
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Sink
import pekko.stream.testkit.scaladsl.StreamTestKit._
import com.couchbase.client.java.auth.PasswordAuthenticator
import com.couchbase.client.java.{ Bucket, CouchbaseCluster }
import com.couchbase.client.java.document.json.JsonObject
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.Future
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CouchbaseSourceSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with CouchbaseSupport
    with Matchers
    with ScalaFutures
    with LogCapturing {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 250.millis)

  "CouchbaseSource" should {

    "run simple Statement Query" in assertAllStagesStopped {
      // #statement
      import com.couchbase.client.java.query.Select.select
      import com.couchbase.client.java.query.dsl.Expression._

      val resultAsFuture: Future[Seq[JsonObject]] =
        CouchbaseSource
          .fromStatement(sessionSettings, select("*").from(i(queryBucketName)).limit(10), bucketName)
          .runWith(Sink.seq)
      // #statement

      resultAsFuture.futureValue.length shouldEqual 4
    }

    // TODO implement overload in CouchbaseSource?
    "allow explicit bucket creation" in assertAllStagesStopped {
      import com.couchbase.client.java.query.Select.select
      import com.couchbase.client.java.query.dsl.Expression._

      val cluster: CouchbaseCluster = CouchbaseCluster.create("localhost")
      cluster.authenticate(new PasswordAuthenticator("Administrator", "password"))
      val bucket: Bucket = cluster.openBucket("pekko")
      val session: CouchbaseSession = CouchbaseSession(bucket)
      actorSystem.registerOnTermination {
        cluster.disconnect()
      }

      val resultAsFuture: Future[Seq[JsonObject]] =
        session
          .streamedQuery(select("*").from(i(queryBucketName)).limit(10))
          .runWith(Sink.seq)

      resultAsFuture.futureValue.length shouldEqual 4
    }

    "run simple N1QL query" in assertAllStagesStopped {

      // #n1ql
      import com.couchbase.client.java.query.{ N1qlParams, N1qlQuery }

      val params = N1qlParams.build.adhoc(false)
      val query = N1qlQuery.simple(s"select count(*) from $queryBucketName", params)

      val resultAsFuture: Future[Seq[JsonObject]] =
        CouchbaseSource
          .fromN1qlQuery(sessionSettings, query, bucketName)
          .runWith(Sink.seq)
      // #n1ql

      resultAsFuture.futureValue.head.get("$1") shouldEqual 4
    }

  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    upsertSampleData(queryBucketName)
  }

  override def afterAll(): Unit = {
    cleanAllInBucket(queryBucketName)
    super.afterAll()
  }

}
