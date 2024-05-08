/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.awsspi.dynamodb

import org.apache.pekko
import pekko.stream.connectors.awsspi.{ PekkoHttpAsyncHttpService, TestBase }
import pekko.util.FutureConverters
import org.scalatest.concurrent.{ Eventually, Futures, IntegrationPatience }
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model._

class ITTestDynamoDB
    extends AnyWordSpec
    with Matchers
    with Futures
    with Eventually
    with IntegrationPatience
    with TestBase {

  def withClient(testCode: DynamoDbAsyncClient => Any): Any = {

    val pekkoClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory().build()

    val client = DynamoDbAsyncClient
      .builder()
      .credentialsProvider(credentialProviderChain)
      .region(defaultRegion)
      .httpClient(pekkoClient)
      .build()

    try
      testCode(client)
    finally { // clean up
      pekkoClient.close()
      client.close()
    }
  }

  "DynamoDB" should {
    "create a table" in withClient { implicit client =>
      val tableName  = s"Movies-${randomIdentifier(5)}"
      val attributes = AttributeDefinition.builder.attributeName("film_id").attributeType(ScalarAttributeType.S).build()
      val keySchema  = KeySchemaElement.builder.attributeName("film_id").keyType(KeyType.HASH).build()

      val result = client
        .createTable(
          CreateTableRequest
            .builder()
            .tableName(tableName)
            .attributeDefinitions(attributes)
            .keySchema(keySchema)
            .provisionedThroughput(
              ProvisionedThroughput.builder
                .readCapacityUnits(1L)
                .writeCapacityUnits(1L)
                .build()
            )
            .build()
        )
        .join

      val desc = result.tableDescription()
      desc.tableName() should be(tableName)

      eventually {
        val response =
          FutureConverters.asScala(client.describeTable(DescribeTableRequest.builder().tableName(tableName).build()))
        response.futureValue.table().tableStatus() should be(TableStatus.ACTIVE)
      }
      FutureConverters.asScala(client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build()))

    }
  }

}
