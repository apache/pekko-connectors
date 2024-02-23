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
import pekko.stream.connectors.couchbase.scaladsl.{ CouchbaseSession, CouchbaseSource }
import pekko.stream.connectors.couchbase.testing.CouchbaseSupport
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Sink
import pekko.stream.testkit.scaladsl.StreamTestKit._
import com.couchbase.client.java.{ AsyncCluster, Bucket, Cluster }
import com.couchbase.client.java.query.QueryResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
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

      val resultAsFuture: Future[Seq[QueryResult]] =
        CouchbaseSource
          .fromQuery(sessionSettings, s"select * from $queryBucketName limit 10")
          .runWith(Sink.seq)
      // #statement

      resultAsFuture.futureValue.length shouldEqual 4
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
