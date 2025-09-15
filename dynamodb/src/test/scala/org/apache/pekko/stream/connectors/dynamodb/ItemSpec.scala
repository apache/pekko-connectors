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
import pekko.stream.connectors.awsspi.PekkoHttpClient
import pekko.stream.connectors.dynamodb.scaladsl._
import pekko.stream.scaladsl.Sink
import pekko.testkit.TestKit
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.TableStatus

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

class ItemSpec extends TestKit(ActorSystem("ItemSpec")) with AsyncWordSpecLike with Matchers with BeforeAndAfterAll {

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

    import ItemSpecOps._

    "1) list zero tables" in {
      DynamoDb.single(listTablesRequest).map(_.tableNames.asScala shouldBe empty)
    }

    "2) create a table" in {
      DynamoDb.single(createTableRequest).map(_.tableDescription.tableStatus shouldBe TableStatus.ACTIVE)
    }

    "3) find a new table" in {
      DynamoDb.single(listTablesRequest).map(_.tableNames.asScala should contain(tableName))
    }

    "4) put an item and read it back" in {
      for {
        _ <- DynamoDb.single(test4PutItemRequest)
        get <- DynamoDb.single(getItemRequest)
      } yield get.item.get("data").s shouldBe "test4data"
    }

    "5) put two items in a batch" in {
      DynamoDb.single(batchWriteItemRequest).map(_.unprocessedItems.size shouldBe 0)
    }

    "6) query two items with page size equal to 1" in {
      DynamoDb
        .source(queryItemsRequest)
        .map(_.items)
        .runWith(Sink.seq)
        .map { results =>
          results.size shouldBe 3
          val Seq(a, b, c) = results: @nowarn("msg=match may not be exhaustive")
          a.size shouldBe 1
          a.get(0).get(sortCol) shouldBe N(0)
          b.size shouldBe 1
          b.get(0).get(sortCol) shouldBe N(1)
          c shouldBe empty
        }
    }

    "7) delete an item" in {
      for {
        _ <- DynamoDb.single(deleteItemRequest)
        get <- DynamoDb.single(getItemRequest)
      } yield get.item shouldBe empty
    }

    // The next 3 tests are ignored as DynamoDB Local does not support transactions; they
    // succeed against a cloud instance so can be enabled once local support is available.

    "8) put two items in a transaction" ignore {
      DynamoDb.single(transactPutItemsRequest).map(_ => succeed)
    }

    "9) get two items in a transaction" ignore {
      DynamoDb.single(transactGetItemsRequest).map { results =>
        val responses = results.responses.asScala
        responses.size shouldBe 2
        responses.head.item.get(sortCol) shouldBe N(0)
        responses.last.item.get(sortCol) shouldBe N(1)
      }
    }

    "10) delete two items in a transaction" ignore {
      DynamoDb.single(transactDeleteItemsRequest).map(_ => succeed)
    }

    "11) delete table" in {
      for {
        _ <- DynamoDb.single(deleteTableRequest)
        list <- DynamoDb.single(listTablesRequest)
      } yield list.tableNames.asScala should not contain tableName
    }

  }

}
