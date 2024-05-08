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

package com.github.pjfanning.pekkohttpspi.s3

import java.io.{File, FileWriter}
import com.github.pjfanning.pekkohttpspi.{PekkoHttpAsyncHttpService, TestBase}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Configuration}
import software.amazon.awssdk.services.s3.model._

import scala.util.Random

class ITTestS3 extends AnyWordSpec with Matchers with TestBase {

  def withClient(checksumEnabled: Boolean = false)(testCode: S3AsyncClient => Any): Any = {

    val pekkoClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory().build()

    val client = S3AsyncClient
      .builder()
      .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(checksumEnabled).build())
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

  "S3 async client" should {
    "upload and download a file to a bucket + cleanup" in withClient(checksumEnabled = true) { implicit client =>
      val bucketName = "aws-spi-test-" + Random.alphanumeric.take(10).filterNot(_.isUpper).mkString
      createBucket(bucketName)
      val randomFile  = File.createTempFile("aws", Random.alphanumeric.take(5).mkString)
      val fileContent = Random.alphanumeric.take(1000).mkString
      val fileWriter  = new FileWriter(randomFile)
      fileWriter.write(fileContent)
      fileWriter.flush()
      client.putObject(PutObjectRequest.builder().bucket(bucketName).key("my-file").build(), randomFile.toPath).join

      val result = client
        .getObject(GetObjectRequest.builder().bucket(bucketName).key("my-file").build(),
                   AsyncResponseTransformer.toBytes[GetObjectResponse]()
        )
        .join
      result.asUtf8String() should be(fileContent)

      client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key("my-file").build()).join()

      client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join()
    }

    "multipart upload" in withClient() { implicit client =>
      val bucketName = "aws-spi-test-" + Random.alphanumeric.take(5).map(_.toLower).mkString
      createBucket(bucketName)
      val fileContent = (0 to 1000000).mkString
      val createMultipartUploadResponse = client
        .createMultipartUpload(
          CreateMultipartUploadRequest.builder().bucket(bucketName).key("bar").contentType("text/plain").build()
        )
        .join()

      val p1 = client
        .uploadPart(
          UploadPartRequest
            .builder()
            .bucket(bucketName)
            .key("bar")
            .partNumber(1)
            .uploadId(createMultipartUploadResponse.uploadId())
            .build(),
          AsyncRequestBody.fromString(fileContent)
        )
        .join
      val p2 = client
        .uploadPart(
          UploadPartRequest
            .builder()
            .bucket(bucketName)
            .key("bar")
            .partNumber(2)
            .uploadId(createMultipartUploadResponse.uploadId())
            .build(),
          AsyncRequestBody.fromString(fileContent)
        )
        .join

      client
        .completeMultipartUpload(
          CompleteMultipartUploadRequest
            .builder()
            .bucket(bucketName)
            .key("bar")
            .uploadId(createMultipartUploadResponse.uploadId())
            .multipartUpload(
              CompletedMultipartUpload
                .builder()
                .parts(CompletedPart.builder().partNumber(1).eTag(p1.eTag()).build(),
                       CompletedPart.builder().partNumber(2).eTag(p2.eTag()).build()
                )
                .build()
            )
            .build()
        )
        .join

      val result = client
        .getObject(GetObjectRequest.builder().bucket(bucketName).key("bar").build(),
                   AsyncResponseTransformer.toBytes[GetObjectResponse]()
        )
        .join
      result.asUtf8String() should be(fileContent + fileContent)

      client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key("bar").build()).join()
      client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join()
    }
  }

  def createBucket(name: String)(implicit client: S3AsyncClient): Unit =
    client.createBucket(CreateBucketRequest.builder().bucket(name).build()).join

}
