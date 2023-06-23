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

package org.apache.pekko.stream.connectors.dynamodb

import java.net.URI

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.dynamodb.scaladsl.DynamoDb
import pekko.testkit.TestKit
import pekko.util.ccompat.JavaConverters._
import com.github.pjfanning.pekkohttpspi.PekkoHttpClient
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import scala.concurrent.ExecutionContext

class TableSpec extends TestKit(ActorSystem("TableSpec")) with AsyncWordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val client: DynamoDbAsyncClient = DynamoDbAsyncClient
    .builder()
    .region(Region.AWS_GLOBAL)
    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
    .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
    .endpointOverride(new URI("http://localhost:8001/"))
    .build()

  override def afterAll(): Unit = {
    client.close()
    shutdown()
    super.afterAll()
  }

  "DynamoDB" should {

    import TableSpecOps._

    "1) create table" in {
      DynamoDb.single(createTableRequest).map(_.tableDescription.tableName shouldBe tableName)
    }

    "2) list tables" in {
      DynamoDb.single(listTablesRequest).map(_.tableNames.asScala should contain(tableName))
    }

    "3) describe table" in {
      DynamoDb.single(describeTableRequest).map(_.table.tableName shouldBe tableName)
    }

    "4) update table" in {
      for {
        describe <- DynamoDb.single(describeTableRequest)
        update <- DynamoDb.single(updateTableRequest)
      } yield {
        describe.table.provisionedThroughput.writeCapacityUnits shouldBe 10L
        update.tableDescription.provisionedThroughput.writeCapacityUnits shouldBe newMaxLimit
      }
    }

    // TODO: Enable this test when DynamoDB Local supports TTLs
    "5) update time to live" ignore {
      for {
        describe <- DynamoDb.single(describeTimeToLiveRequest)
        update <- DynamoDb.single(updateTimeToLiveRequest)
      } yield {
        describe.timeToLiveDescription.attributeName shouldBe empty
        update.timeToLiveSpecification.attributeName shouldBe "expires"
      }
    }

    "6) delete table" in {
      for {
        _ <- DynamoDb.single(deleteTableRequest)
        list <- DynamoDb.single(listTablesRequest)
      } yield list.tableNames.asScala should not contain tableName
    }

  }

}
