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

package org.apache.pekko.stream.connectors.googlecloud.storage.impl

import java.util.UUID
import org.apache.pekko
import pekko.http.scaladsl.model.ContentTypes
import pekko.stream.connectors.googlecloud.storage.WithMaterializerGlobal
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.util.ByteString
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.testkit.scaladsl.LogCapturing

import scala.concurrent.Future

trait GCStorageStreamIntegrationSpec
    extends AnyWordSpec
    with WithMaterializerGlobal
    with BeforeAndAfter
    with Matchers
    with ScalaFutures
    with LogCapturing {

  private implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 60.seconds, interval = 60.millis)

  var folderName: String = _

  def testFileName(file: String): String = folderName + file

  def settings: GoogleSettings

  def bucket: String
  def rewriteBucket: String
  def projectId: String

  before {
    folderName = classOf[GCStorageStreamIntegrationSpec].getSimpleName + UUID.randomUUID().toString + "/"
  }

  after {
    Await.result(GCStorageStream.deleteObjectsByPrefixSource(bucket, Some(folderName)).runWith(Sink.seq), 10.seconds)
  }

  "GCStorageStream" should {

    "be able to create and delete a bucket" in {
      val randomBucketName = s"pekko-connectors_${UUID.randomUUID().toString}"

      val res = for {
        bucket <- GCStorageStream.createBucketSource(randomBucketName, "europe-west1").runWith(Sink.head)
        afterCreate <- GCStorageStream.getBucketSource(bucket.name).runWith(Sink.head)
        _ <- GCStorageStream.deleteBucketSource(bucket.name).runWith(Sink.head)
        afterDelete <- GCStorageStream.getBucketSource(bucket.name).runWith(Sink.head)
      } yield (bucket, afterCreate, afterDelete)

      val (bucket, afterCreate, afterDelete) = res.futureValue
      bucket.kind shouldBe "storage#bucket"
      afterCreate.isDefined shouldBe true
      afterDelete shouldBe None
    }

    "be able to get bucket info" in {
      // the bucket is no longer empty
      val bucketInfo = GCStorageStream
        .getBucketSource(bucket)
        .runWith(Sink.head)
      bucketInfo.futureValue.map(_.kind) shouldBe Some("storage#bucket")
    }

    "be able to list an empty bucket" in {
      // the bucket is no longer empty
      val objects = GCStorageStream
        .listBucket(bucket, None)
        .runWith(Sink.seq)
      objects.futureValue shouldBe empty
    }

    "get an empty list when listing a non existing folder" in {
      val objects = GCStorageStream
        .listBucket(bucket, Some("non-existent"))
        .runWith(Sink.seq)

      objects.futureValue shouldBe empty
    }

    "be able to list an existing folder" in {
      val listing = for {
        _ <- GCStorageStream
          .putObject(bucket,
            testFileName("testa.txt"),
            Source.single(ByteString("testa")),
            ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        _ <- GCStorageStream
          .putObject(bucket,
            testFileName("testb.txt"),
            Source.single(ByteString("testa")),
            ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        listing <- GCStorageStream.listBucket(bucket, Some(folderName)).runWith(Sink.seq)
      } yield {
        listing
      }

      listing.futureValue should have size 2
    }

    "get metadata of an existing file" in {
      val content = ByteString("metadata file")

      val option = for {
        _ <- GCStorageStream
          .putObject(bucket, testFileName("metadata-file"), Source.single(content), ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        option <- GCStorageStream.getObject(bucket, testFileName("metadata-file")).runWith(Sink.head)
      } yield option

      val so = option.futureValue.get
      so.name shouldBe testFileName("metadata-file")
      so.size shouldBe content.size
      so.contentType shouldBe ContentTypes.`text/plain(UTF-8)`
    }

    "get none when asking metadata of non-existing file" in {
      val option = GCStorageStream.getObject(bucket, testFileName("metadata-file")).runWith(Sink.head)
      option.futureValue shouldBe None
    }

    "be able to upload a file" in {
      val fileName = testFileName("test-file")
      val res = for {
        so <- GCStorageStream
          .putObject(
            bucket,
            fileName,
            Source.single(ByteString(Random.alphanumeric.take(50000).map(c => c.toByte).toArray)),
            ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        listing <- GCStorageStream.listBucket(bucket, Some(folderName)).runWith(Sink.seq)
      } yield (so, listing)

      val (so, listing) = res.futureValue

      so.name shouldBe fileName
      so.size shouldBe 50000
      listing should have size 1
    }

    "be able to download an existing file" in {
      val fileName = testFileName("test-file")
      val content = ByteString(Random.alphanumeric.take(50000).map(c => c.toByte).toArray)
      val bs = for {
        _ <- GCStorageStream
          .putObject(bucket, fileName, Source.single(content), ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        bs <- GCStorageStream
          .download(bucket, fileName)
          .runWith(Sink.head)
          .flatMap(
            _.map(_.runWith(Sink.fold(ByteString.empty) { _ ++ _ })).getOrElse(Future.successful(ByteString.empty)))
      } yield bs
      bs.futureValue shouldBe content
    }

    "get a None when downloading a non extisting file" in {
      val fileName = testFileName("non-existing-file")
      val download = GCStorageStream
        .download(bucket, fileName)
        .runWith(Sink.head)
        .futureValue

      download shouldBe None
    }

    "get a single empty ByteString when downloading a non existing file" in {
      val fileName = testFileName("non-existing-file")
      val res = for {
        _ <- GCStorageStream
          .putObject(bucket, fileName, Source.single(ByteString.empty), ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        res <- GCStorageStream
          .download(bucket, fileName)
          .runWith(Sink.head)
          .flatMap(
            _.map(_.runWith(Sink.fold(ByteString.empty) { _ ++ _ })).getOrElse(Future.successful(ByteString.empty)))
      } yield res
      res.futureValue shouldBe ByteString.empty
    }

    "delete an existing file" in {
      val result = for {
        _ <- GCStorageStream
          .putObject(bucket,
            testFileName("fileToDelete"),
            Source.single(ByteString("File content")),
            ContentTypes.`text/plain(UTF-8)`)
          .runWith(Sink.head)
        result <- GCStorageStream.deleteObjectSource(bucket, testFileName("fileToDelete")).runWith(Sink.head)
      } yield result
      result.futureValue shouldBe true
    }

    "delete an unexisting file should not give an error" in {
      val result =
        GCStorageStream.deleteObjectSource(bucket, testFileName("non-existing-file-to-delete")).runWith(Sink.head)
      result.futureValue shouldBe false
    }

    "provide a sink to stream data to gcs" in {
      val fileName = testFileName("big-streaming-file")
      val meta = Map("meta-key-1" -> "value-1")

      val sink =
        GCStorageStream.resumableUpload(bucket, fileName, ContentTypes.`text/plain(UTF-8)`, 4 * 256 * 1024, Some(meta))

      val res = Source
        .fromIterator(() =>
          Iterator.fill[ByteString](10) {
            ByteString(Random.alphanumeric.take(1234567).map(c => c.toByte).toArray)
          })
        .runWith(sink)

      val so = res.futureValue
      so.name shouldBe fileName
      so.size shouldBe 12345670
      so.metadata shouldBe Some(meta)
    }

    "rewrite file from source to destination path" in {
      val fileName = "big-streaming-file"

      val sink =
        GCStorageStream.resumableUpload(bucket, fileName, ContentTypes.`text/plain(UTF-8)`, 4 * 256 * 1024)

      val uploadResult = Source
        .fromIterator(() =>
          Iterator.fill[ByteString](10) {
            ByteString(Random.alphanumeric.take(1234567).map(c => c.toByte).toArray)
          })
        .runWith(sink)

      val res = for {
        _ <- uploadResult
        res <- GCStorageStream.rewrite(bucket, fileName, rewriteBucket, fileName).run()
      } yield res

      res.futureValue.name shouldBe fileName
      res.futureValue.bucket shouldBe rewriteBucket
      GCStorageStream.getObject(rewriteBucket, fileName).runWith(Sink.head).futureValue.isDefined shouldBe true

      GCStorageStream.deleteObjectSource(bucket, fileName).runWith(Sink.head).futureValue
      GCStorageStream.deleteObjectSource(rewriteBucket, fileName).runWith(Sink.head).futureValue
    }
  }
}
