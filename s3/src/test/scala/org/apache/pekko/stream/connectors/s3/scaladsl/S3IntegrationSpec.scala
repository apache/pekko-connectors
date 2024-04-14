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

package org.apache.pekko.stream.connectors.s3.scaladsl

import org.apache.pekko
import org.scalacheck.Gen
import pekko.actor.ActorSystem
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model.{ ContentTypes, StatusCodes }
import pekko.stream.{ Attributes, KillSwitches, SharedKillSwitch }
import pekko.stream.connectors.s3.AccessStyle.{ PathAccessStyle, VirtualHostAccessStyle }
import pekko.stream.connectors.s3.BucketAccess.{ AccessGranted, NotExists }
import pekko.stream.connectors.s3._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Keep, Sink, Source }
import pekko.testkit.{ TestKit, TestKitBase }
import pekko.util.ccompat.JavaConverters._
import pekko.util.ByteString
import pekko.{ Done, NotUsed }
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import software.amazon.awssdk.auth.credentials._
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers._

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

trait S3IntegrationSpec
    extends AnyFlatSpecLike
    with TestKitBase
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with ScalaCheckPropertyChecks
    with OptionValues
    with LogCapturing {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val defaultPatience: PatienceConfig = PatienceConfig(3.minutes, 100.millis)

  /**
   * A prefix that will get added to each generated bucket in the test, this is to track the buckets that are
   * specifically created by the test
   */
  lazy val bucketPrefix: Option[String] = None

  /**
   * Whether to randomly generate bucket names
   */
  val randomlyGenerateBucketNames: Boolean

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)

  /**
   * Whether to enable cleanup of buckets after tests are run and if so the initial delay to wait after the test
   */
  lazy val enableCleanup: Option[FiniteDuration] = None

  /**
   * The MaxTimeout when cleaning up all of the buckets during `afterAll`
   */
  lazy val maxCleanupTimeout: FiniteDuration = 10.minutes

  /**
   * @param constantBucketName The bucket name constant to use if we are not randomly generating bucket names
   */
  def genBucketName(constantBucketName: String): Gen[String] =
    if (randomlyGenerateBucketNames)
      Generators.bucketNameGen(useVirtualDotHost = false, bucketPrefix)
    else
      Gen.const(bucketPrefix.getOrElse("") ++ constantBucketName)

  def createBucket(bucket: String, versioning: Boolean, bucketReference: AtomicReference[String], s3Attrs: Attributes)
      : Future[Unit] =
    for {
      bucketResponse <- S3.checkIfBucketExists(bucket)(implicitly, s3Attrs)
      _ <- bucketResponse match {
        case BucketAccess.AccessDenied =>
          throw new RuntimeException(
            s"Unable to create bucket: $bucket since it already exists however permissions are inadequate")
        case BucketAccess.AccessGranted | BucketAccess.NotExists =>
          for {
            _ <- bucketResponse match {
              case BucketAccess.AccessGranted =>
                system.log.info(
                  s"Deleting and recreating bucket: $bucket since it already exists with correct permissions")
                TestUtils.cleanAndDeleteBucket(bucket, s3Attrs)
              case _ =>
                Future.successful(())
            }
            _ <- S3.makeBucket(bucket)(implicitly, s3Attrs)
            // TODO: Figure out a way to properly test this with Minio, see https://github.com/akka/alpakka/issues/2750
            _ <- if (versioning && this.isInstanceOf[AWSS3IntegrationSpec])
              S3.putBucketVersioning(bucket, BucketVersioning().withStatus(BucketVersioningStatus.Enabled))(implicitly,
                s3Attrs)
            else
              Future.successful(())
            _ = bucketReference.set(bucket)
          } yield ()
      }
    } yield ()

  private val defaultBucketReference = new AtomicReference[String]()
  def withDefaultBucket(testCode: String => Assertion): Assertion =
    testCode(defaultBucketReference.get())

  // with dots forcing path style access
  private val bucketWithDotsReference = new AtomicReference[String]()
  def withBucketWithDots(testCode: String => Assertion): Assertion =
    testCode(bucketWithDotsReference.get())

  private val nonExistingBucketReference = new AtomicReference[String]()
  def withNonExistingBucket(testCode: String => Assertion): Assertion =
    testCode(nonExistingBucketReference.get())

  private val bucketWithVersioningReference = new AtomicReference[String]()
  def withBucketWithVersioning(testCode: String => Assertion): Assertion =
    testCode(bucketWithVersioningReference.get())

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (bucketWithDots, defaultBucket, bucketWithVersioning, nonExistingBucket) = {
      val baseGen = for {
        bucketWithDots <- genBucketName(S3IntegrationSpec.BucketWithDots)
        defaultBucket <- genBucketName(S3IntegrationSpec.DefaultBucket)
        bucketWithVersioning <- genBucketName(S3IntegrationSpec.BucketWithVersioning)
        nonExistingBucket <- genBucketName(S3IntegrationSpec.NonExistingBucket)
      } yield (bucketWithDots, defaultBucket, bucketWithVersioning, nonExistingBucket)

      if (randomlyGenerateBucketNames)
        TestUtils.loopUntilGenRetrievesValue(baseGen.filter {
          case (bucketWithDots, defaultBucket, bucketWithVersioning, nonExistingBucket) =>
            // Make sure that we don't somehow generate 2 or more buckets with the same name
            List(bucketWithDots, defaultBucket, bucketWithVersioning, nonExistingBucket).distinct.size == 4
        })
      else
        baseGen.sample.get
    }

    val makeBuckets = for {
      _ <-
        createBucket(bucketWithDots, versioning = false, bucketWithDotsReference,
          attributes(
            _.withS3RegionProvider(
              new AwsRegionProvider {
                val getRegion: Region = Region.EU_CENTRAL_1
              })))
      _ <- createBucket(defaultBucket, versioning = false, defaultBucketReference, attributes)
      _ <- createBucket(bucketWithVersioning, versioning = true, bucketWithVersioningReference, attributes)
      _ = nonExistingBucketReference.set(nonExistingBucket)
    } yield ()

    system.log.info(
      s"Corresponding S3 bucket names are bucketWithDots: $bucketWithDots, defaultBucket: $defaultBucket, bucketWithVersioning: $bucketWithVersioning, nonExistingBucket: $nonExistingBucket")

    Await.result(makeBuckets, 10.seconds)
  }

  val objectKey = "test"

  val objectValue = "Some String"
  val metaHeaders: Map[String, String] = Map("location" -> "Africa", "datatype" -> "image")

  private def cleanBucket(bucket: String, attributes: Attributes): Future[Unit] = (for {
    check <- S3.checkIfBucketExists(bucket)(implicitly, attributes)
    _ <- check match {
      case BucketAccess.AccessDenied =>
        Future {
          system.log.warning(
            s"Cannot delete bucket: $bucket due to having access denied. Please look into this as it can fill up your AWS account")
        }
      case BucketAccess.AccessGranted =>
        system.log.info(s"Cleaning up bucket: $bucket")
        TestUtils.cleanAndDeleteBucket(bucket, attributes)
      case BucketAccess.NotExists =>
        Future {
          system.log.info(s"Not deleting bucket: $bucket since it no longer exists")
        }
    }
  } yield ()).recover { case util.control.NonFatal(error) =>
    system.log.error(s"Error deleting bucket: $bucket", error)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    enableCleanup.foreach { timeout =>
      val bucketsWithAttributes = List(
        Option(defaultBucketReference.get()).map(bucket => (bucket, attributes)),
        Option(bucketWithDotsReference.get()).map(bucket =>
          (bucket,
            attributes(
              _.withS3RegionProvider(
                new AwsRegionProvider {
                  val getRegion: Region = Region.EU_CENTRAL_1
                })))),
        Option(bucketWithVersioningReference.get()).map(bucket => (bucket, attributes))).flatten

      Await.result(Future.traverse(bucketsWithAttributes) { case (bucket, attrs) => cleanBucket(bucket, attrs) },
        timeout)
    }
    Http(system)
      .shutdownAllConnectionPools()
      .foreach(_ => TestKit.shutdownActorSystem(system))
  }

  lazy val defaultS3Settings: S3Settings =
    S3Settings()
      .withS3RegionProvider(
        new AwsRegionProvider {
          val getRegion: Region = Region.US_EAST_1
        })
      .withAccessStyle(PathAccessStyle)

  def attributes: Attributes =
    S3Attributes.settings(defaultS3Settings)

  def attributes(block: S3Settings => S3Settings): Attributes =
    S3Attributes.settings(
      block(defaultS3Settings))

  def otherRegionSettingsPathStyleAccess =
    attributes(
      _.withAccessStyle(PathAccessStyle)
        .withS3RegionProvider(new AwsRegionProvider {
          val getRegion: Region = Region.EU_CENTRAL_1
        }))

  def invalidCredentials: Attributes =
    attributes(
      _.withCredentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create("invalid", "invalid"))))

  def defaultRegionContentCount = 0
  def otherRegionContentCount = 0

  it should "list buckets in current aws account" in withDefaultBucket { defaultBucket =>
    assume((this.isInstanceOf[AWSS3IntegrationSpec], S3IntegrationSpec.AWSS3EnableListAllMyBucketsTests) match {
      case (true, true)  => true
      case (true, false) => false
      case (false, _)    => true
    })
    val result = for {
      buckets <- S3.listBuckets().withAttributes(attributes).runWith(Sink.seq)
    } yield buckets

    val buckets = result.futureValue

    buckets.map(_.name) should contain(defaultBucket)
  }

  it should "list buckets in current AWS account using non US_EAST_1 region" in withDefaultBucket { defaultBucket =>
    assume((this.isInstanceOf[AWSS3IntegrationSpec], S3IntegrationSpec.AWSS3EnableListAllMyBucketsTests) match {
      case (true, true)  => true
      case (true, false) => false
      case (false, _)    => true
    })
    // Its only AWS that complains if listBuckets is called from a non US_EAST_1 region
    val result = for {
      buckets <- S3.listBuckets().withAttributes(
        S3Attributes.settings(defaultS3Settings.withS3RegionProvider(new AwsRegionProvider {
          override def getRegion: Region = Region.EU_CENTRAL_1
        }))).runWith(Sink.seq)
    } yield buckets

    val buckets = result.futureValue

    buckets.map(_.name) should contain(defaultBucket)
  }

  it should "list with real credentials" in withDefaultBucket { defaultBucket =>
    val result = S3
      .listBucket(defaultBucket, None)
      .withAttributes(attributes)
      .runWith(Sink.seq)

    val listingResult = result.futureValue
    listingResult.size shouldBe defaultRegionContentCount
  }

  it should "list with real credentials using the Version 1 API" in withDefaultBucket { defaultBucket =>
    val result = S3
      .listBucket(defaultBucket, None)
      .withAttributes(attributes(_.withListBucketApiVersion(ApiVersion.ListBucketVersion1)))
      .runWith(Sink.seq)

    val listingResult = result.futureValue
    listingResult.size shouldBe defaultRegionContentCount
  }

  it should "list with real credentials in non us-east-1 zone" in withBucketWithDots { bucketWithDots =>
    val result = S3
      .listBucket(bucketWithDots, None)
      .withAttributes(otherRegionSettingsPathStyleAccess)
      .runWith(Sink.seq)

    result.futureValue.size shouldBe otherRegionContentCount
  }

  it should "upload with real credentials" in withDefaultBucket { defaultBucket =>
    val objectKey = "putTest"
    val bytes = ByteString(objectValue)
    val data = Source.single(ByteString(objectValue))

    val result =
      S3.putObject(defaultBucket,
        objectKey,
        data,
        bytes.length,
        s3Headers = S3Headers().withMetaHeaders(MetaHeaders(metaHeaders)))
        .withAttributes(attributes)
        .runWith(Sink.head)

    result.futureValue.eTag should not be empty
  }

  it should "upload and delete" in withDefaultBucket { defaultBucket =>
    val objectKey = "putTest"
    val bytes = ByteString(objectValue)
    val data = Source.single(ByteString(objectValue))

    val result = for {
      put <- S3
        .putObject(defaultBucket,
          objectKey,
          data,
          bytes.length,
          s3Headers = S3Headers().withMetaHeaders(MetaHeaders(metaHeaders)))
        .withAttributes(attributes)
        .runWith(Sink.head)
      metaBefore <- S3.getObjectMetadata(defaultBucket, objectKey).withAttributes(attributes).runWith(Sink.head)
      delete <- S3.deleteObject(defaultBucket, objectKey).withAttributes(attributes).runWith(Sink.head)
      metaAfter <- S3.getObjectMetadata(defaultBucket, objectKey).withAttributes(attributes).runWith(Sink.head)
    } yield (put, delete, metaBefore, metaAfter)

    val (putResult, _, metaBefore, metaAfter) = result.futureValue
    putResult.eTag should not be empty
    metaBefore should not be empty
    metaBefore.get.contentType shouldBe Some(ContentTypes.`application/octet-stream`.value)
    metaAfter shouldBe empty
  }

  it should "upload multipart with real credentials" in withDefaultBucket { defaultBucket =>
    val source: Source[ByteString, Any] = Source(ByteString(objectValue) :: Nil)

    val result =
      source
        .runWith(
          S3.multipartUpload(defaultBucket, objectKey, metaHeaders = MetaHeaders(metaHeaders))
            .withAttributes(attributes))

    val multipartUploadResult = result.futureValue
    multipartUploadResult.bucket shouldBe defaultBucket
    multipartUploadResult.key shouldBe objectKey
  }

  it should "download with real credentials" in withDefaultBucket { defaultBucket =>
    val (metaFuture, bodyFuture) = S3
      .getObject(defaultBucket, objectKey)
      .withAttributes(attributes)
      .map(_.decodeString("utf8"))
      .toMat(Sink.head)(Keep.both)
      .run()

    bodyFuture.futureValue shouldBe objectValue
    val meta = metaFuture.futureValue
    meta.eTag should not be empty
    meta.contentType shouldBe Some(ContentTypes.`application/octet-stream`.value)
  }

  it should "delete with real credentials" in withDefaultBucket { defaultBucket =>
    val delete = S3
      .deleteObject(defaultBucket, objectKey)
      .withAttributes(attributes)
      .runWith(Sink.head)
    delete.futureValue shouldEqual pekko.Done
  }

  it should "upload huge multipart with real credentials" in withDefaultBucket { defaultBucket =>
    val objectKey = "huge"
    val hugeString = "0123456789abcdef" * 64 * 1024 * 11
    val result =
      Source
        .single(ByteString(hugeString))
        .runWith(
          S3.multipartUpload(defaultBucket, objectKey, metaHeaders = MetaHeaders(metaHeaders))
            .withAttributes(attributes))

    val multipartUploadResult = result.futureValue
    multipartUploadResult.bucket shouldBe defaultBucket
    multipartUploadResult.key shouldBe objectKey
  }

  it should "upload, download and delete with spaces in the key" in withDefaultBucket { defaultBucket =>
    val objectKey = "test folder/test file.txt"
    val source: Source[ByteString, Any] = Source(ByteString(objectValue) :: Nil)

    val results = for {
      upload <- source
        .runWith(
          S3.multipartUpload(defaultBucket, objectKey, metaHeaders = MetaHeaders(metaHeaders))
            .withAttributes(attributes))
      download <- S3
        .getObject(defaultBucket, objectKey)
        .withAttributes(attributes)
        .map(_.decodeString("utf8"))
        .runWith(Sink.head)
    } yield (upload, download)

    val (multipartUploadResult, downloaded) = results.futureValue

    multipartUploadResult.bucket shouldBe defaultBucket
    multipartUploadResult.key shouldBe objectKey
    downloaded shouldBe objectValue

    S3.deleteObject(defaultBucket, objectKey)
      .withAttributes(attributes)
      .runWith(Sink.head)
      .futureValue shouldEqual pekko.Done
  }

  it should "upload, download and delete with brackets in the key" in withDefaultBucket { defaultBucket =>
    val objectKey = "abc/DEF/2017/06/15/1234 (1).TXT"
    val source: Source[ByteString, Any] = Source(ByteString(objectValue) :: Nil)

    val results = for {
      upload <- source
        .runWith(
          S3.multipartUpload(defaultBucket, objectKey, metaHeaders = MetaHeaders(metaHeaders))
            .withAttributes(attributes))
      download <- S3
        .getObject(defaultBucket, objectKey)
        .withAttributes(attributes)
        .map(_.decodeString("utf8"))
        .runWith(Sink.head)
    } yield (upload, download)

    val (multipartUploadResult, downloaded) = results.futureValue

    multipartUploadResult.bucket shouldBe defaultBucket
    multipartUploadResult.key shouldBe objectKey
    downloaded shouldBe objectValue

    S3.deleteObject(defaultBucket, objectKey)
      .withAttributes(attributes)
      .runWith(Sink.head)
      .futureValue shouldEqual pekko.Done
  }

  it should "upload, download and delete with spaces in the key in non us-east-1 zone" in withBucketWithDots {
    bucketWithDots =>
      uploadDownloadAndDeleteInOtherRegionCase(bucketWithDots,
        "test folder/test file.txt")
  }

  // we want ASCII and other UTF-8 characters!
  it should "upload, download and delete with special characters in the key in non us-east-1 zone" in withBucketWithDots {
    bucketWithDots =>
      uploadDownloadAndDeleteInOtherRegionCase(bucketWithDots,
        "føldęrü/1234()[]><!? .TXT")
  }

  it should "upload, download and delete with `+` character in the key in non us-east-1 zone" in withBucketWithDots {
    bucketWithDots =>
      uploadDownloadAndDeleteInOtherRegionCase(bucketWithDots,
        "1 + 2 = 3")
  }

  it should "upload, copy, download the copy, and delete" in withDefaultBucket { defaultBucket =>
    uploadCopyDownload(defaultBucket,
      "original/file.txt",
      "copy/file.txt")
  }

  // NOTE: MinIO currently has problems copying files with spaces.
  it should "upload, copy, download the copy, and delete with special characters in key" in withDefaultBucket {
    defaultBucket =>
      uploadCopyDownload(defaultBucket,
        "original/føldęrü/1234()[]><!?.TXT",
        "copy/1 + 2 = 3")
  }

  it should "upload 2 files with common prefix, 1 with different prefix and delete by prefix" in withDefaultBucket {
    defaultBucket =>
      val sourceKey1 = "original/file1.txt"
      val sourceKey2 = "original/file2.txt"
      val sourceKey3 = "uploaded/file3.txt"
      val source: Source[ByteString, Any] = Source(ByteString(objectValue) :: Nil)

      val results = for {
        upload1 <- source.runWith(S3.multipartUpload(defaultBucket, sourceKey1).withAttributes(attributes))
        upload2 <- source.runWith(S3.multipartUpload(defaultBucket, sourceKey2).withAttributes(attributes))
        upload3 <- source.runWith(S3.multipartUpload(defaultBucket, sourceKey3).withAttributes(attributes))
      } yield (upload1, upload2, upload3)

      whenReady(results) {
        case (upload1, upload2, upload3) =>
          upload1.bucket shouldEqual defaultBucket
          upload1.key shouldEqual sourceKey1
          upload2.bucket shouldEqual defaultBucket
          upload2.key shouldEqual sourceKey2
          upload3.bucket shouldEqual defaultBucket
          upload3.key shouldEqual sourceKey3

          S3.deleteObjectsByPrefix(defaultBucket, Some("original"))
            .withAttributes(attributes)
            .runWith(Sink.ignore)
            .futureValue shouldEqual pekko.Done
          val numOfKeysForPrefix =
            S3.listBucket(defaultBucket, Some("original"))
              .withAttributes(attributes)
              .runFold(0)((result, _) => result + 1)
              .futureValue
          numOfKeysForPrefix shouldEqual 0
          S3.deleteObject(defaultBucket, sourceKey3)
            .withAttributes(attributes)
            .runWith(Sink.head)
            .futureValue shouldEqual pekko.Done
      }
  }

  it should "create multiple versions of an object and successfully clean it with deleteBucketContents" in withBucketWithVersioning {
    bucketWithVersioning =>
      // TODO: Figure out a way to properly test this with Minio, see https://github.com/akka/alpakka/issues/2750
      assume(this.isInstanceOf[AWSS3IntegrationSpec])
      val versionKey = "test-version"
      val one = ByteString("one")
      val two = ByteString("two")
      val three = ByteString("three")

      val results =
        for {
          // Clean the bucket just incase there is residual data in there
          _ <- S3
            .deleteBucketContents(bucketWithVersioning, deleteAllVersions = true)
            .withAttributes(attributes)
            .runWith(Sink.ignore)
          _ <- S3
            .putObject(bucketWithVersioning, versionKey, Source.single(one), one.length, s3Headers = S3Headers())
            .withAttributes(attributes)
            .runWith(Sink.ignore)
          _ <- S3
            .putObject(bucketWithVersioning, versionKey, Source.single(two), two.length, s3Headers = S3Headers())
            .withAttributes(attributes)
            .runWith(Sink.ignore)
          _ <- S3
            .putObject(bucketWithVersioning, versionKey, Source.single(three), three.length, s3Headers = S3Headers())
            .withAttributes(attributes)
            .runWith(Sink.ignore)
          versionsBeforeDelete <- S3
            .listObjectVersions(bucketWithVersioning, None)
            .withAttributes(attributes)
            .runWith(Sink.seq)
          _ <- S3
            .deleteBucketContents(bucketWithVersioning, deleteAllVersions = true)
            .withAttributes(attributes)
            .runWith(Sink.ignore)
          versionsAfterDelete <- S3
            .listObjectVersions(bucketWithVersioning, None)
            .withAttributes(attributes)
            .runWith(Sink.seq)
          listBucketContentsAfterDelete <- S3
            .listBucket(bucketWithVersioning, None)
            .withAttributes(attributes)
            .runWith(Sink.seq)

        } yield (versionsBeforeDelete.flatMap { case (versions, _) => versions },
          versionsAfterDelete.flatMap {
            case (versions, _) => versions
          }, listBucketContentsAfterDelete)

      val (versionsBeforeDelete, versionsAfterDelete, bucketContentsAfterDelete) = results.futureValue

      versionsBeforeDelete.size shouldEqual 3
      versionsAfterDelete.size shouldEqual 0
      bucketContentsAfterDelete.size shouldEqual 0
  }

  it should "listing object versions for a non versioned bucket should return None for versionId" in withDefaultBucket {
    defaultBucket =>
      // TODO: Figure out a way to properly test this with Minio, see https://github.com/akka/alpakka/issues/2750
      assume(this.isInstanceOf[AWSS3IntegrationSpec])
      val objectKey = "listObjectVersionIdTest"
      val bytes = ByteString(objectValue)
      val data = Source.single(ByteString(objectValue))
      val results = for {
        _ <- S3
          .putObject(defaultBucket,
            objectKey,
            data,
            bytes.length,
            s3Headers = S3Headers().withMetaHeaders(MetaHeaders(metaHeaders)))
          .withAttributes(attributes)
          .runWith(Sink.ignore)
        result <- S3.listObjectVersions(defaultBucket, None).withAttributes(attributes).runWith(Sink.seq)
        _ <- S3.deleteObject(defaultBucket, objectKey).withAttributes(attributes).runWith(Sink.ignore)
      } yield result.flatMap { case (versions, _) => versions }

      Inspectors.forEvery(results.futureValue) { version =>
        version.versionId shouldEqual None
      }
  }

  it should "upload 2 files, delete all files in bucket" in withDefaultBucket { defaultBucket =>
    val sourceKey1 = "original/file1.txt"
    val sourceKey2 = "original/file2.txt"
    val source: Source[ByteString, Any] = Source(ByteString(objectValue) :: Nil)

    val results = for {
      upload1 <- source.runWith(S3.multipartUpload(defaultBucket, sourceKey1).withAttributes(attributes))
      upload2 <- source.runWith(S3.multipartUpload(defaultBucket, sourceKey2).withAttributes(attributes))
    } yield (upload1, upload2)

    whenReady(results) {
      case (upload1, upload2) =>
        upload1.bucket shouldEqual defaultBucket
        upload1.key shouldEqual sourceKey1
        upload2.bucket shouldEqual defaultBucket
        upload2.key shouldEqual sourceKey2

        S3.deleteObjectsByPrefix(defaultBucket, prefix = None)
          .withAttributes(attributes)
          .runWith(Sink.ignore)
          .futureValue shouldEqual pekko.Done
        val numOfKeysForPrefix =
          S3.listBucket(defaultBucket, None)
            .withAttributes(attributes)
            .runFold(0)((result, _) => result + 1)
            .futureValue
        numOfKeysForPrefix shouldEqual 0
    }
  }

  @tailrec
  final def createStringCollectionWithMinChunkSizeRec(numberOfChunks: Int,
      stringAcc: BigInt = BigInt(0),
      currentChunk: Int = 0,
      currentChunkSize: Int = 0,
      result: Vector[ByteString] = Vector.empty): Vector[ByteString] =
    if (currentChunk == numberOfChunks)
      result
    else {
      val newAcc = stringAcc + 1

      result.lift(currentChunk) match {
        case Some(currentString) =>
          val newString = ByteString(s"\n${newAcc.toString()}")
          if (currentChunkSize < S3.MinChunkSize) {
            val appendedString = currentString ++ newString
            val newChunkSize = currentChunkSize + newString.length
            createStringCollectionWithMinChunkSizeRec(numberOfChunks,
              newAcc,
              currentChunk,
              newChunkSize,
              result.updated(currentChunk, appendedString))
          } else {
            val newChunk = currentChunk + 1
            val (newResult, newChunkSize) =
              // // We are at the last index at this point so don't append a new entry at the end of the Vector
              if (currentChunk == numberOfChunks - 1)
                (result, currentChunkSize)
              else
                (result :+ newString, newString.length)
            createStringCollectionWithMinChunkSizeRec(numberOfChunks, newAcc, newChunk, newChunkSize, newResult)
          }
        case None =>
          // This case happens right at the start
          val initial = ByteString("1")
          val firstResult = Vector(initial)
          createStringCollectionWithMinChunkSizeRec(numberOfChunks, newAcc, currentChunk, initial.length, firstResult)
      }
    }

  /**
   * Creates a `List` of `ByteString` where the size of each ByteString is guaranteed to be at least `S3.MinChunkSize`
   * in size.
   *
   * This is useful for tests that deal with multipart uploads, since S3 persists a part everytime it receives
   * `S3.MinChunkSize` bytes
   * @param numberOfChunks The number of chunks to create
   * @return A List of `ByteString` where each element is at least `S3.MinChunkSize` in size
   */
  def createStringCollectionWithMinChunkSize(numberOfChunks: Int): List[ByteString] =
    createStringCollectionWithMinChunkSizeRec(numberOfChunks).toList

  case object AbortException extends Exception("Aborting multipart upload")

  def createSlowSource(data: immutable.Seq[ByteString],
      killSwitch: Option[SharedKillSwitch]): Source[ByteString, NotUsed] = {
    val base = Source(data)
      .throttle(1, 10.seconds)

    killSwitch.fold(base)(ks => base.viaMat(ks.flow)(Keep.left))
  }

  def byteStringToMD5(byteString: ByteString): String = {
    import java.math.BigInteger
    import java.security.MessageDigest

    val digest = MessageDigest.getInstance("MD5")
    digest.update(byteString.asByteBuffer)
    String.format("%032X", new BigInteger(1, digest.digest())).toLowerCase
  }

  it should "upload 1 file slowly, cancel it and retrieve a multipart upload + list part" in withDefaultBucket {
    defaultBucket =>
      // This test doesn't work on Minio since minio doesn't properly implement this API, see
      // https://github.com/minio/minio/issues/13246
      assume(this.isInstanceOf[AWSS3IntegrationSpec])
      val sourceKey = "original/file-slow.txt"
      val sharedKillSwitch = KillSwitches.shared("abort-multipart-upload")

      val inputData = createStringCollectionWithMinChunkSize(5)
      val slowSource = createSlowSource(inputData, Some(sharedKillSwitch))

      val multiPartUpload =
        slowSource.toMat(S3.multipartUpload(defaultBucket, sourceKey).withAttributes(attributes))(Keep.right).run()

      val results = for {
        _ <- pekko.pattern.after(25.seconds)(Future {
          sharedKillSwitch.abort(AbortException)
        })
        _ <- multiPartUpload.recover {
          case AbortException => ()
        }
        incomplete <- S3.listMultipartUpload(defaultBucket, None).withAttributes(attributes).runWith(Sink.seq)
        uploadIds = incomplete.collect {
          case uploadPart if uploadPart.key == sourceKey => uploadPart.uploadId
        }
        parts <- Future.sequence(uploadIds.map { uploadId =>
          S3.listParts(defaultBucket, sourceKey, uploadId).withAttributes(attributes).runWith(Sink.seq)
        })
        // Cleanup the uploads after
        _ <- Future.sequence(uploadIds.map { uploadId =>
          S3.deleteUpload(defaultBucket, sourceKey, uploadId)(implicitly, attributes)
        })
      } yield (uploadIds, incomplete, parts.flatten)

      whenReady(results) {
        case (uploadIds, incompleteFiles, parts) =>
          val inputsUntilAbort = inputData.slice(0, 3)
          incompleteFiles.exists(_.key == sourceKey) shouldBe true
          parts.nonEmpty shouldBe true
          uploadIds.size shouldBe 1
          parts.size shouldBe 3
          parts.map(_.size) shouldBe inputsUntilAbort.map(_.utf8String.getBytes("UTF-8").length)
          // In S3 the etag's are actually an MD5 hash of the contents of the part so we can use this to check
          // that the data has been uploaded correctly and in the right order, see
          // https://docs.aws.amazon.com/AmazonS3/latest/API/RESTCommonResponseHeaders.html
          parts.map(_.eTag.replaceAll("\"", "")) shouldBe inputsUntilAbort.map(byteStringToMD5)
      }
  }

  it should "upload 1 file slowly, cancel it and then resume it to complete the upload" in withDefaultBucket {
    defaultBucket =>
      // This test doesn't work on Minio since minio doesn't properly implement this API, see
      // https://github.com/minio/minio/issues/13246
      assume(this.isInstanceOf[AWSS3IntegrationSpec])
      val sourceKey = "original/file-slow-2.txt"
      val sharedKillSwitch = KillSwitches.shared("abort-multipart-upload-2")

      val inputData = createStringCollectionWithMinChunkSize(6)

      val slowSource = createSlowSource(inputData, Some(sharedKillSwitch))

      val multiPartUpload =
        slowSource
          .toMat(S3.multipartUpload(defaultBucket, sourceKey).withAttributes(attributes))(
            Keep.right)
          .run()

      val results = for {
        _ <- pekko.pattern.after(25.seconds)(Future {
          sharedKillSwitch.abort(AbortException)
        })
        _ <- multiPartUpload.recover {
          case AbortException => ()
        }
        incomplete <- S3.listMultipartUpload(defaultBucket, None).withAttributes(attributes).runWith(Sink.seq)

        uploadId = incomplete.collectFirst {
          case uploadPart if uploadPart.key == sourceKey => uploadPart.uploadId
        }.get

        parts <- S3.listParts(defaultBucket, sourceKey, uploadId).withAttributes(attributes).runWith(Sink.seq)

        remainingData = inputData.slice(3, 6)
        _ <- Source(remainingData)
          .toMat(
            S3.resumeMultipartUpload(defaultBucket, sourceKey, uploadId, parts.map(_.toPart))
              .withAttributes(attributes))(
            Keep.right)
          .run()

        // This delay is here because sometimes there is a delay when you complete a large file and its
        // actually downloadable
        downloaded <- pekko.pattern.after(5.seconds)(
          S3.getObject(defaultBucket, sourceKey).withAttributes(attributes).runWith(Sink.seq))

        _ <- S3.deleteObject(defaultBucket, sourceKey).withAttributes(attributes).runWith(Sink.head)
      } yield downloaded

      whenReady(results) { downloads =>
        val fullDownloadedFile = downloads.fold(ByteString.empty)(_ ++ _)
        val fullInputData = inputData.fold(ByteString.empty)(_ ++ _)

        fullInputData.utf8String shouldEqual fullDownloadedFile.utf8String
      }
  }

  it should "upload a full file but complete it manually with S3.completeMultipartUploadSource" in withDefaultBucket {
    defaultBucket =>
      assume(this.isInstanceOf[AWSS3IntegrationSpec])
      val sourceKey = "original/file-slow-3.txt"
      val sharedKillSwitch = KillSwitches.shared("abort-multipart-upload-3")

      val inputData = createStringCollectionWithMinChunkSize(4)

      val slowSource = createSlowSource(inputData, Some(sharedKillSwitch))

      val multiPartUpload =
        slowSource
          .toMat(S3.multipartUpload(defaultBucket, sourceKey).withAttributes(attributes))(
            Keep.right)
          .run()

      val results = for {
        _ <- pekko.pattern.after(25.seconds)(Future {
          sharedKillSwitch.abort(AbortException)
        })
        _ <- multiPartUpload.recover {
          case AbortException => ()
        }
        incomplete <- S3.listMultipartUpload(defaultBucket, None).withAttributes(attributes).runWith(Sink.seq)

        uploadId = incomplete.collectFirst {
          case uploadPart if uploadPart.key == sourceKey => uploadPart.uploadId
        }.get

        parts <- S3.listParts(defaultBucket, sourceKey, uploadId).withAttributes(attributes).runWith(Sink.seq)

        _ <- S3.completeMultipartUpload(defaultBucket, sourceKey, uploadId, parts.map(_.toPart))(implicitly, attributes)
        // This delay is here because sometimes there is a delay when you complete a large file and its
        // actually downloadable
        downloaded <- pekko.pattern.after(5.seconds)(
          S3.getObject(defaultBucket, sourceKey).withAttributes(attributes).runWith(Sink.seq))
        _ <- S3.deleteObject(defaultBucket, sourceKey).withAttributes(attributes).runWith(Sink.head)
      } yield downloaded

      whenReady(results) { downloads =>
        val fullDownloadedFile = downloads.fold(ByteString.empty)(_ ++ _)
        val fullInputData = inputData.slice(0, 3).fold(ByteString.empty)(_ ++ _)

        fullInputData.utf8String shouldEqual fullDownloadedFile.utf8String
      }
  }
  @tailrec
  final def createStringCollectionContextWithMinChunkSizeRec(
      numberOfChunks: Int,
      stringAcc: BigInt = BigInt(0),
      currentChunk: Int = 0,
      currentChunkSize: Int = 0,
      result: Vector[Vector[(ByteString, BigInt)]] = Vector.empty): Vector[Vector[(ByteString, BigInt)]] =
    if (currentChunk == numberOfChunks)
      result
    else {
      val newAcc = stringAcc + 1

      result.lift(currentChunk) match {
        case Some(currentStrings) =>
          val newString = ByteString(s"\n${newAcc.toString()}")
          if (currentChunkSize < S3.MinChunkSize) {
            val newChunkSize = currentChunkSize + newString.length
            val newEntry = currentStrings :+ ((newString, newAcc))
            createStringCollectionContextWithMinChunkSizeRec(numberOfChunks,
              newAcc,
              currentChunk,
              newChunkSize,
              result.updated(currentChunk, newEntry))
          } else {
            val newChunk = currentChunk + 1
            val (newResult, newChunkSize) =
              // // We are at the last index at this point so don't append a new entry at the end of the Vector
              if (currentChunk == numberOfChunks - 1)
                (result, currentChunkSize)
              else
                (result :+ Vector((newString, newAcc)), newString.length)
            createStringCollectionContextWithMinChunkSizeRec(numberOfChunks, newAcc, newChunk, newChunkSize, newResult)
          }
        case None =>
          // This case happens right at the start
          val initial = ByteString("1")
          val firstResult = Vector(Vector((initial, newAcc)))
          createStringCollectionContextWithMinChunkSizeRec(numberOfChunks,
            newAcc,
            currentChunk,
            initial.length,
            firstResult)
      }
    }

  /**
   * Creates a `List` of `List[(ByteString, BigInt)]` where the accumulated size of each ByteString in the list is
   * guaranteed to be at least `S3.MinChunkSize` in size.
   *
   * This is useful for tests that deal with multipart uploads, since S3 persists a part everytime it receives
   * `S3.MinChunkSize` bytes. Unlike `createStringCollectionWithMinChunkSizeRec` this version also adds an index
   * to each individual `ByteString` which is helpful when dealing with testing for context
   * @param numberOfChunks The number of chunks to create
   * @return A List of `List[ByteString]` where each element is at least `S3.MinChunkSize` in size along with an
   *         incrementing index for each `ByteString`
   */
  def createStringCollectionContextWithMinChunkSize(numberOfChunks: Int): List[List[(ByteString, BigInt)]] =
    createStringCollectionContextWithMinChunkSizeRec(numberOfChunks).toList.map(_.toList)

  it should "perform a chunked multi-part upload with the correct context" in withDefaultBucket { defaultBucket =>
    val sourceKey = "original/file-context-1.txt"
    val inputData = createStringCollectionContextWithMinChunkSize(4)
    val source = Source(inputData.flatten)
    val originalContexts = inputData.map(_.map { case (_, contexts) => contexts })
    val originalData = inputData.flatten.map { case (data, _) => data }.fold(ByteString.empty)(_ ++ _)
    val resultingContexts = new ConcurrentLinkedQueue[List[BigInt]]()
    val collectContextSink = Sink
      .foreach[(UploadPartResponse, immutable.Iterable[BigInt])] {
        case (_, contexts) =>
          resultingContexts.add(contexts.toList)
      }
      .mapMaterializedValue(_ => NotUsed)

    val results = for {
      _ <- source
        .toMat(
          S3.multipartUploadWithContext[BigInt](defaultBucket, sourceKey, collectContextSink, chunkingParallelism = 1)
            .withAttributes(attributes))(
          Keep.right)
        .run()
      // This delay is here because sometimes there is a delay when you complete a large file and its
      // actually downloadable
      downloaded <- pekko.pattern.after(5.seconds)(
        S3.getObject(defaultBucket, sourceKey).withAttributes(attributes).runWith(Sink.seq))
      _ <- S3.deleteObject(defaultBucket, sourceKey).withAttributes(attributes).runWith(Sink.head)

    } yield downloaded

    whenReady(results) { downloads =>
      val fullDownloadedFile = downloads.fold(ByteString.empty)(_ ++ _)
      originalData.utf8String shouldEqual fullDownloadedFile.utf8String
      originalContexts shouldEqual resultingContexts.asScala.toList
    }
  }

  it should "make a bucket with given name" in {
    forAll(genBucketName("samplebucket1")) { bucketName =>
      system.log.info(s"Created bucket with name: $bucketName")

      implicit val attr: Attributes = attributes
      val request: Future[Done] = S3
        .makeBucket(bucketName)

      whenReady(request) { value =>
        value shouldEqual Done

        S3.deleteBucket(bucketName).futureValue shouldBe Done
      }
    }
  }

  it should "throw an exception while creating a bucket with the same name in Minio" in withDefaultBucket {
    defaultBucket =>
      // S3 will only throw a BucketAlreadyExists exception if the owner of the account creating the bucket with an
      // already existing name different from the owner of the already existing bucket. On the other hand Minio will
      // always throw this exception. Since its hard to test the S3 case of different owners we only run this test
      // against Minio.
      assume(this.isInstanceOf[MinioS3IntegrationSpec])
      implicit val attr: Attributes = attributes
      S3.makeBucket(defaultBucket).failed.futureValue shouldBe an[S3Exception]
  }

  it should "Do nothing while creating a bucket with the same name and owner in AWS S3" in withDefaultBucket {
    defaultBucket =>
      // Due to this being a PUT request, S3 doesn't actually throw an exception unless
      // you are NOT the owner of the already existing bucket.
      // See https://github.com/aws/aws-sdk-go/issues/1362#issuecomment-722554726
      assume(this.isInstanceOf[AWSS3IntegrationSpec])
      implicit val attr: Attributes = attributes
      S3.makeBucket(defaultBucket).futureValue shouldBe Done
  }

  it should "enable and disable versioning for a bucket" in {
    // TODO: Figure out a way to properly test this with Minio, see https://github.com/akka/alpakka/issues/2750
    assume(this.isInstanceOf[AWSS3IntegrationSpec])
    forAll(genBucketName("samplebucket1")) { bucketName =>
      implicit val attr: Attributes = attributes

      val request = for {
        _ <- S3.makeBucket(bucketName)
        firstResult <- S3.getBucketVersioning(bucketName)
        _ <- S3.putBucketVersioning(bucketName, BucketVersioning().withStatus(BucketVersioningStatus.Enabled))
        secondResult <- S3.getBucketVersioning(bucketName)
      } yield (firstResult, secondResult)

      whenReady(request) { case (firstValue, secondValue) =>
        firstValue shouldEqual BucketVersioningResult()
        firstValue.bucketVersioningEnabled shouldEqual false
        secondValue shouldEqual BucketVersioningResult().withStatus(BucketVersioningStatus.Enabled)
        secondValue.bucketVersioningEnabled shouldEqual true
        S3.putBucketVersioning(bucketName,
          BucketVersioning().withStatus(BucketVersioningStatus.Suspended)).futureValue shouldBe Done
        S3.deleteBucket(bucketName).futureValue
      }
    }
  }

  it should "enable and disable versioning for a bucket with MFA delete configured to false" in {
    assume((this.isInstanceOf[AWSS3IntegrationSpec], S3IntegrationSpec.AWSS3EnableMFATests) match {
      case (true, true)  => true
      case (true, false) => false
      case (false, _)    => true
    })
    // TODO: Figure out a way to properly test this with Minio, see https://github.com/akka/alpakka/issues/2750
    assume(this.isInstanceOf[AWSS3IntegrationSpec])
    forAll(genBucketName("samplebucketversioningmfadeletefalse")) { bucketName =>
      implicit val attr: Attributes = attributes

      val request = for {
        _ <- S3.makeBucket(bucketName)
        _ <- S3.putBucketVersioning(bucketName,
          BucketVersioning()
            .withStatus(BucketVersioningStatus.Enabled)
            .withMfaDelete(MFAStatus.Disabled))
        result <- S3.getBucketVersioning(bucketName)
      } yield result

      whenReady(request) { value =>
        value shouldEqual BucketVersioningResult().withStatus(BucketVersioningStatus.Enabled).withMfaDelete(false)
        S3.putBucketVersioning(bucketName,
          BucketVersioning().withStatus(BucketVersioningStatus.Suspended)).futureValue shouldBe Done
        S3.deleteBucket(bucketName).futureValue
      }
    }
  }

  it should "create and delete bucket with a given name" in {
    forAll(genBucketName("samplebucket3")) { bucketName =>
      system.log.info(s"Created bucket with name: $bucketName")

      val makeRequest: Source[Done, NotUsed] = S3
        .makeBucketSource(bucketName)
        .withAttributes(attributes)
      val deleteRequest: Source[Done, NotUsed] = S3
        .deleteBucketSource(bucketName)
        .withAttributes(attributes)

      val request = for {
        make <- makeRequest.runWith(Sink.ignore)
        delete <- deleteRequest.runWith(Sink.ignore)
      } yield (make, delete)

      request.futureValue should equal((Done, Done))
    }
  }

  it should "create a bucket in the non default us-east-1 region" in {
    forAll(genBucketName("samplebucketotherregion")) { bucketName =>
      system.log.info(s"Created bucket with name: $bucketName")

      val request = for {
        _ <- S3
          .makeBucketSource(bucketName)
          .withAttributes(otherRegionSettingsPathStyleAccess)
          .runWith(Sink.head)
        result <- S3
          .checkIfBucketExistsSource(bucketName)
          .withAttributes(otherRegionSettingsPathStyleAccess)
          .runWith(Sink.head)
        _ <- S3
          .deleteBucketSource(bucketName)
          .withAttributes(otherRegionSettingsPathStyleAccess)
          .runWith(Sink.head)
      } yield result

      request.futureValue shouldEqual AccessGranted
    }
  }

  it should "throw an exception while deleting bucket that doesn't exist" in withNonExistingBucket {
    nonExistingBucket =>
      implicit val attr: Attributes = attributes
      S3.deleteBucket(nonExistingBucket).failed.futureValue shouldBe an[S3Exception]
  }

  it should "check if bucket exists" in withDefaultBucket { defaultBucket =>
    implicit val attr: Attributes = attributes
    val checkIfBucketExits: Future[BucketAccess] = S3.checkIfBucketExists(defaultBucket)

    whenReady(checkIfBucketExits) { bucketState =>
      bucketState should equal(AccessGranted)
    }
  }

  it should "check for non-existing bucket" in withNonExistingBucket { nonExistingBucket =>
    implicit val attr: Attributes = attributes
    val request: Future[BucketAccess] = S3.checkIfBucketExists(nonExistingBucket)

    whenReady(request) { response =>
      response should equal(NotExists)
    }
  }

  it should "contain error code even if exception in empty" in withDefaultBucket { defaultBucket =>
    val exception =
      S3.getObjectMetadata(defaultBucket, "sample")
        .withAttributes(invalidCredentials)
        .runWith(Sink.head)
        .failed
        .mapTo[S3Exception]
        .futureValue

    exception.code shouldBe StatusCodes.Forbidden.toString()
  }

  private val chunk: ByteString = ByteString.fromArray(Array.fill(S3.MinChunkSize)(0.toByte))

  it should "only upload single chunk when size of the ByteString equals chunk size" in withDefaultBucket {
    defaultBucket =>
      val source: Source[ByteString, Any] = Source.single(chunk)
      uploadAndAndCheckParts(defaultBucket, source, 1)
  }

  it should "only upload single chunk when exact chunk is followed by an empty ByteString" in withDefaultBucket {
    defaultBucket =>
      val source: Source[ByteString, Any] = Source[ByteString](
        chunk :: ByteString.empty :: Nil)

      uploadAndAndCheckParts(defaultBucket, source, 1)
  }

  it should "upload two chunks size of ByteStrings equals chunk size" in withDefaultBucket { defaultBucket =>
    val source: Source[ByteString, Any] = Source(chunk :: chunk :: Nil)
    uploadAndAndCheckParts(defaultBucket, source, 2)
  }

  it should "upload empty source" in withDefaultBucket { defaultBucket =>
    val upload =
      for {
        upload <- Source
          .empty[ByteString]
          .runWith(
            S3.multipartUpload(defaultBucket, objectKey, chunkSize = S3.MinChunkSize)
              .withAttributes(attributes))
        _ <- S3.deleteObject(defaultBucket, objectKey).withAttributes(attributes).runWith(Sink.head)
      } yield upload

    upload.futureValue.eTag should not be empty
  }

  private def uploadAndAndCheckParts(
      defaultBucket: String, source: Source[ByteString, _], expectedParts: Int): Assertion = {
    val metadata =
      for {
        _ <- source.runWith(
          S3.multipartUpload(defaultBucket, objectKey, chunkSize = S3.MinChunkSize)
            .withAttributes(attributes))
        metadata <- S3
          .getObjectMetadata(defaultBucket, objectKey)
          .withAttributes(attributes)
          .runWith(Sink.head)
        _ <- S3.deleteObject(defaultBucket, objectKey).withAttributes(attributes).runWith(Sink.head)
      } yield metadata

    val etag = metadata.futureValue.get.eTag.get
    etag.substring(etag.indexOf('-') + 1).toInt shouldBe expectedParts
  }

  private def uploadDownloadAndDeleteInOtherRegionCase(bucketWithDots: String, objectKey: String): Assertion = {
    val source: Source[ByteString, Any] = Source(ByteString(objectValue) :: Nil)

    val results = for {
      upload <- source
        .runWith(
          S3.multipartUpload(bucketWithDots, objectKey, metaHeaders = MetaHeaders(metaHeaders))
            .withAttributes(otherRegionSettingsPathStyleAccess))
      download <- S3
        .getObject(bucketWithDots, objectKey)
        .withAttributes(otherRegionSettingsPathStyleAccess)
        .map(_.decodeString("utf8"))
        .runWith(Sink.head)
    } yield (upload, download)

    val (multipartUploadResult, downloaded) = results.futureValue

    multipartUploadResult.bucket shouldBe bucketWithDots
    multipartUploadResult.key shouldBe objectKey
    downloaded shouldBe objectValue

    S3.deleteObject(bucketWithDots, objectKey)
      .withAttributes(otherRegionSettingsPathStyleAccess)
      .runWith(Sink.head)
      .futureValue shouldEqual pekko.Done
  }

  private def uploadCopyDownload(defaultBucket: String, sourceKey: String, targetKey: String): Assertion = {
    val source: Source[ByteString, Any] = Source.single(ByteString(objectValue))

    val results = for {
      upload <- source.runWith(S3.multipartUpload(defaultBucket, sourceKey).withAttributes(attributes))
      copy <- S3
        .multipartCopy(defaultBucket, sourceKey, defaultBucket, targetKey)
        .withAttributes(attributes)
        .run()
      download <- S3
        .getObject(defaultBucket, targetKey)
        .withAttributes(attributes)
        .map(_.decodeString("utf8"))
        .runWith(Sink.head)
    } yield (upload, copy, download)

    whenReady(results) {
      case (upload, copy, downloaded) =>
        upload.bucket shouldEqual defaultBucket
        upload.key shouldEqual sourceKey
        copy.bucket shouldEqual defaultBucket
        copy.key shouldEqual targetKey
        downloaded shouldBe objectValue

        S3.deleteObject(defaultBucket, sourceKey)
          .withAttributes(attributes)
          .runWith(Sink.head)
          .futureValue shouldEqual pekko.Done
        S3.deleteObject(defaultBucket, targetKey)
          .withAttributes(attributes)
          .runWith(Sink.head)
          .futureValue shouldEqual pekko.Done
    }
  }
}

/*
 * This is an integration test. In order for the test suite to run make sure you have the necessary
 * permissions to create/delete buckets and objects in different regions.
 *
 * Set your keys aws access-key-id and secret-access-key in src/test/resources/application.conf
 *
 * Since this a test that is marked with `@DoNotDiscover` it will not run as part of the test suite unless its
 * explicitly executed with
 * `sbt "s3/Test/runMain org.scalatest.tools.Runner -o -s org.apache.pekko.stream.connectors.s3.scaladsl.AWSS3IntegrationSpec"`
 *
 * See https://www.scalatest.org/user_guide/using_the_runner for more details about the runner.
 *
 * Running tests directly via IDE's such as Intellij is also supported
 */
@DoNotDiscover
class AWSS3IntegrationSpec extends TestKit(ActorSystem("AWSS3IntegrationSpec")) with S3IntegrationSpec {
  // Its recommended when testing against S3 to use a specific prefix in order to identify where the buckets
  // are coming from since typically S3 accounts can also in other contexts
  override lazy val bucketPrefix: Option[String] =
    sys.props.get("pekko.stream.connectors.s3.scaladsl.AWSS3IntegrationSpec.bucketPrefix").orElse(
      Some("pekko-connectors-"))

  override lazy val enableCleanup: Option[FiniteDuration] =
    sys.props.get("pekko.stream.connectors.s3.scaladsl.AWSS3IntegrationSpec.enableCleanup").map(
      Duration.apply).collect {
      case finiteDuration: FiniteDuration => finiteDuration
    }.orElse(Some(1.minute))

  // Since S3 accounts share global state, we should randomly generate bucket names so concurrent tests
  // against an S3 account don't conflict with each other
  override val randomlyGenerateBucketNames: Boolean =
    sys.props.get("pekko.stream.connectors.s3.scaladsl.AWSS3IntegrationSpec.randomlyGenerateBucketNames").forall(
      _.toBoolean)
}

/*
 * For this test, you need a have docker installed and running.
 *
 * Run the tests from inside sbt:
 * s3/testOnly *.MinioS3IntegrationSpec
 */

class MinioS3IntegrationSpec
    extends TestKit(ActorSystem("MinioS3IntegrationSpec"))
    with S3IntegrationSpec
    with MinioS3Test {

  // Since a unique new Minio container is started with each test run there is no point in making random
  // bucket names
  override val randomlyGenerateBucketNames: Boolean = false

  override lazy val defaultS3Settings: S3Settings = s3Settings
    .withS3RegionProvider(
      new AwsRegionProvider {
        val getRegion: Region = Region.US_EAST_1
      })
    .withAccessStyle(PathAccessStyle)

  it should "properly set the endpointUrl using VirtualHostAccessStyle" in {
    s3Settings
      .withAccessStyle(VirtualHostAccessStyle)
      .withEndpointUrl(container.getVirtualHost)
      .endpointUrl
      .value shouldEqual container.getVirtualHost
  }
}

object S3IntegrationSpec {
  val BucketWithDots = "my.test.frankfurt"
  val DefaultBucket = "my-test-us-east-1"
  val BucketWithVersioning = "my-bucket-with-versioning"
  val NonExistingBucket = "nowhere"

  val AWSS3EnableListAllMyBucketsTests =
    sys.props.get("pekko.stream.connectors.s3.scaladsl.AWSS3IntegrationSpec.enableListAllMyBucketsTests").forall(
      _.toBoolean)

  val AWSS3EnableMFATests =
    sys.props.get("pekko.stream.connectors.s3.scaladsl.AWSS3IntegrationSpec.enableMFATests").forall(_.toBoolean)
}
