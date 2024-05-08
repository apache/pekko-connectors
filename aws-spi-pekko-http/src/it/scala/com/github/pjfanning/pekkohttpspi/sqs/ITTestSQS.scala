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

package com.github.pjfanning.pekkohttpspi.sqs

import com.github.pjfanning.pekkohttpspi.{PekkoHttpAsyncHttpService, TestBase}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{
  CreateQueueRequest,
  DeleteQueueRequest,
  ReceiveMessageRequest,
  SendMessageRequest
}

import scala.util.Random

class ITTestSQS extends AnyWordSpec with Matchers with TestBase {

  def withClient(testCode: SqsAsyncClient => Any): Any = {

    val pekkoClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory().build()

    val client = SqsAsyncClient
      .builder()
      .credentialsProvider(credentialProviderChain)
      .httpClient(pekkoClient)
      .region(defaultRegion)
      .build()

    try
      testCode(client)
    finally { // clean up
      pekkoClient.close()
      client.close()
    }
  }

  "Async SQS client" should {

    "publish a message to a queue" in withClient { implicit client =>
      val queueName     = "aws-spi-test-" + Random.alphanumeric.take(10).filterNot(_.isUpper).mkString
      val queueResponse = client.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).join()
      val queueUrl      = queueResponse.queueUrl()
      client.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody("123").build()).join()
      val receivedMessage =
        client.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(1).build()).join()
      receivedMessage.messages().get(0).body() should be("123")

      // deleteQueue
      client.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build()).join()

      val queueListing = client.listQueues().join()
      queueListing.queueUrls() shouldBe java.util.Collections.EMPTY_LIST

    }
  }

}
