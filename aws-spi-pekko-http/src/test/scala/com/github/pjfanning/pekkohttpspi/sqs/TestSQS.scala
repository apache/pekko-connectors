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

import com.github.pjfanning.pekkohttpspi.{PekkoHttpAsyncHttpService, ElasticMQSQSBaseAwsClientTest}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

class TestSQS extends ElasticMQSQSBaseAwsClientTest[SqsAsyncClient] {
  "Async SQS client" should {

    "publish a message to a queue" in withClient { implicit client =>
      client.createQueue(CreateQueueRequest.builder().queueName("foo").build()).join()
      client
        .sendMessage(SendMessageRequest.builder().queueUrl(s"$endpoint/queue/foo").messageBody("123").build())
        .join()
      val receivedMessage = client
        .receiveMessage(ReceiveMessageRequest.builder().queueUrl(s"$endpoint/queue/foo").maxNumberOfMessages(1).build())
        .join()
      receivedMessage.messages().get(0).body() should be("123")
    }

    "delete a message" in withClient { implicit client =>
      client.createQueue(CreateQueueRequest.builder().queueName("foo").build()).join()
      client
        .sendMessage(SendMessageRequest.builder().queueUrl(s"$endpoint/queue/foo").messageBody("123").build())
        .join()

      val receivedMessages = client
        .receiveMessage(ReceiveMessageRequest.builder().queueUrl(s"$endpoint/queue/foo").maxNumberOfMessages(1).build())
        .join

      client
        .deleteMessage(
          DeleteMessageRequest
            .builder()
            .queueUrl(s"$endpoint/queue/foo")
            .receiptHandle(receivedMessages.messages().get(0).receiptHandle())
            .build()
        )
        .join()

      val receivedMessage = client
        .receiveMessage(
          ReceiveMessageRequest
            .builder()
            .queueUrl(s"$endpoint/queue/foo")
            .maxNumberOfMessages(1)
            .waitTimeSeconds(1)
            .build()
        )
        .join()
      receivedMessage.messages() shouldBe java.util.Collections.EMPTY_LIST
    }

  }

  def withClient(testCode: SqsAsyncClient => Any): Any = {

    val pekkoClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory().build()

    val client = SqsAsyncClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
      .httpClient(pekkoClient)
      .region(defaultRegion)
      .endpointOverride(endpoint)
      .build()

    try
      testCode(client)
    finally { // clean up
      pekkoClient.close()
      client.close()
    }
  }

  override def service: String = "sqs"
}
