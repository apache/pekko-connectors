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

import java.time.{ Duration, Instant }

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.MediaTypes
import pekko.stream.connectors.s3._
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Source
import pekko.testkit.TestKit
import pekko.util.ByteString
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

/*
 * Test performing a multipart upload against a slow minio instance.  When minio is responding to a
 * CompleteMultipartUpload request that takes longer than 10 seconds to complete, it will set the ContentType header
 * to be `text/event-stream` and inject spaces in the response to prevent the client connection from timing out
 * (see https://github.com/minio/minio/blob/fbd1c5f51a900158013e8d8e4593d9ca898f8b7e/cmd/object-handlers.go#L2444-L2479)
 *
 * For this test, you need a local minio instance with i/o throttling enabled so that the CompleteMultipartUpload takes
 * longer than 10 seconds to process.
 * See `create-slow-minio.sh` for an example on how to setup minio with throttling.
 *
 * To run this test, first uncomment the @Ignore annotation, then run this command inside sbt:
 * s3/testOnly *.S3SlowMinioIntegrationSpec
 */
@Ignore
class S3SlowMinioIntegrationSpec
    extends AnyFlatSpecLike
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with OptionValues
    with LogCapturing {

  implicit val actorSystem: ActorSystem = ActorSystem(
    "S3SlowMinioIntegrationSpec",
    config().withFallback(ConfigFactory.load()))
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val defaultBucket = "my-test-us-east-1"

  implicit val defaultPatience: PatienceConfig = PatienceConfig(90.seconds, 100.millis)

  override protected def beforeAll(): Unit = {
    Await.ready(S3.makeBucket(defaultBucket).recover { case _ => Done }, 10.seconds)
  }
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(actorSystem)

  def config(): Config = {
    val accessKey = "TESTKEY"
    val secret = "TESTSECRET"
    val endpointUrlPathStyle = "http://localhost:9001"

    ConfigFactory.parseString(s"""
         |pekko.connectors.s3 {
         |  aws {
         |    credentials {
         |      provider = static
         |      access-key-id = $accessKey
         |      secret-access-key = $secret
         |    }
         |    region {
         |      provider = static
         |      default-region = "us-east-1"
         |    }
         |  }
         |  path-style-access = force
         |  endpoint-url = "$endpointUrlPathStyle"
         |}
    """.stripMargin)
  }

  it should "have the default bucket" in {
    S3.checkIfBucketExists(defaultBucket).futureValue shouldBe BucketAccess.AccessGranted
  }

  it should "upload huge multipart to a slow server" in {
    val objectKey = "slow"
    val hugeString = "0123456789abcdef" * 64 * 1024 * 11 // ~ 11mb

    val result =
      Source
        .single(ByteString(hugeString))
        .runWith {
          S3.multipartUpload(defaultBucket, objectKey, MediaTypes.`application/octet-stream`)
        }

    val start = Instant.now()
    val multipartUploadResult = result.futureValue
    multipartUploadResult.bucket shouldBe defaultBucket
    multipartUploadResult.key shouldBe objectKey
    Duration.between(start, Instant.now()) should be > Duration.ofSeconds(12)
  }
}
