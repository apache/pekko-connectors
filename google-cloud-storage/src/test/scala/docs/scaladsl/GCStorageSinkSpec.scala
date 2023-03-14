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

import org.apache.pekko.http.scaladsl.model.ContentTypes
import org.apache.pekko.stream.connectors.googlecloud.storage.StorageObject
import org.apache.pekko.stream.connectors.googlecloud.storage.scaladsl.{ GCStorage, GCStorageWiremockBase }
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.util.Random

class GCStorageSinkSpec
    extends GCStorageWiremockBase
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with LogCapturing {

  override def beforeAll(): Unit = ()

  override protected def afterAll(): Unit = {
    super.afterAll()
    this.stopWireMockServer()
  }

  "upload large file" in {
    val chunkSize = 256 * 1024
    val firstChunkContent = Random.alphanumeric.take(chunkSize).mkString
    val secondChunkContent = Random.alphanumeric.take(chunkSize).mkString
    val metadata = Map(Random.alphanumeric.take(5).mkString -> Random.alphanumeric.take(5).mkString)

    mock.simulate(
      mockTokenApi,
      mockLargeFileUpload(firstChunkContent, secondChunkContent, chunkSize, Some(metadata)))

    // #upload
    val sink =
      GCStorage.resumableUpload(bucketName, fileName, ContentTypes.`text/plain(UTF-8)`, chunkSize, metadata)

    val source = Source(
      List(ByteString(firstChunkContent), ByteString(secondChunkContent)))

    val result: Future[StorageObject] = source.runWith(sink)

    // #upload

    val storageObject: StorageObject = result.futureValue

    storageObject.name shouldBe fileName
    storageObject.bucket shouldBe bucketName
    storageObject.metadata shouldBe Some(metadata)
  }

  // The new ResumableUpload API automatically resumes interrupted/failed uploads
  "fail with error when large file upload fails" ignore {
    val chunkSize = 256 * 1024
    val firstChunkContent = Random.alphanumeric.take(chunkSize).mkString
    val secondChunkContent = Random.alphanumeric.take(chunkSize).mkString

    mock.simulate(
      mockTokenApi,
      mockLargeFileUploadFailure(firstChunkContent, secondChunkContent, chunkSize))

    val sink =
      GCStorage.resumableUpload(bucketName, fileName, ContentTypes.`text/plain(UTF-8)`, chunkSize)

    val source = Source(List(ByteString(firstChunkContent), ByteString(secondChunkContent)))
    source
      .runWith(sink)
      .failed
      .futureValue
      .getMessage shouldBe "Uploading part failed with status 400 Bad Request: Chunk upload failed"
  }

  "rewrite file" in {
    val rewriteBucketName = "alpakka-rewrite"

    mock.simulate(
      mockTokenApi,
      mockRewrite(rewriteBucketName))

    // #rewrite

    val result: Future[StorageObject] = GCStorage.rewrite(bucketName, fileName, rewriteBucketName, fileName).run()

    // #rewrite

    val storageObject = result.futureValue

    storageObject.name shouldBe fileName
    storageObject.bucket shouldBe rewriteBucketName
  }

  "fail when rewrite file fails" in {
    val rewriteBucketName = "alpakka-rewrite"

    mock.simulate(
      mockTokenApi,
      mockRewriteFailure(rewriteBucketName))

    val result = GCStorage.rewrite(bucketName, fileName, rewriteBucketName, fileName).run()

    result.failed.futureValue.getMessage shouldBe "[400] Rewrite failed"
  }

}
