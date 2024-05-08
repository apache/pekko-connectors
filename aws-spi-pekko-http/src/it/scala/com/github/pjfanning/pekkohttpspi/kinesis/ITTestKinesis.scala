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

package com.github.pjfanning.pekkohttpspi.kinesis

import com.github.pjfanning.pekkohttpspi.{PekkoHttpAsyncHttpService, TestBase}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model._

import scala.util.Random

class ITTestKinesis extends AnyWordSpec with Matchers with TestBase {

  def withClient(testCode: KinesisAsyncClient => Any): Any = {

    val pekkoClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory().build()

    val client = KinesisAsyncClient
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

  "Kinesis async client" should {

    "use a data stream: create + put + get + delete" in withClient { implicit client =>
      val streamName = "aws-spi-test-" + Random.alphanumeric.take(10).filterNot(_.isUpper).mkString
      val data       = "123"

      val createRequest = CreateStreamRequest
        .builder()
        .streamName(streamName)
        .shardCount(1)
        .build()

      val _                     = client.createStream(createRequest).join()
      val describeStreamRequest = DescribeStreamRequest.builder().streamName(streamName).build()

      Thread.sleep(5000)
      val streamArn = waitToBeCreated(client, describeStreamRequest, 15)

      val putRequest = PutRecordRequest
        .builder()
        .streamName(streamName)
        .partitionKey("partitionKey")
        .data(SdkBytes.fromUtf8String(data))
        .build()

      val putResponse = client.putRecord(putRequest).join()

      val getShardIterator = GetShardIteratorRequest
        .builder()
        .streamName(streamName)
        .shardId(putResponse.shardId())
        .shardIteratorType("TRIM_HORIZON")
        .build()
      val shardIterator = client.getShardIterator(getShardIterator).join()

      val getRecordsRequest = GetRecordsRequest.builder().shardIterator(shardIterator.shardIterator()).limit(1).build()

      Thread.sleep(3000)

      val res = client.getRecords(getRecordsRequest).join()

      res.records().get(0).data().asUtf8String() shouldBe data

      client.deleteStream(DeleteStreamRequest.builder().streamName(streamName).build()).join()

    }
  }

  private def waitToBeCreated(client: KinesisAsyncClient, req: DescribeStreamRequest, tries: Int): String =
    if (tries == 0) "error"
    else {
      val r = client.describeStream(req).join()
      if (r.streamDescription().streamStatus() == StreamStatus.ACTIVE) {
        println(s"Current status: ${r.streamDescription().streamStatus()}")
        r.streamDescription().streamARN()
      } else {
        Thread.sleep(1000)
        waitToBeCreated(client, req, tries - 1)
      }
    }
}
