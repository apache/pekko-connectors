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

package org.apache.pekko.stream.connectors.s3

import org.apache.pekko.testkit.TestKitBase
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.Suite
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }

trait MinioS3Test extends ForAllTestContainer with TestKitBase { self: Suite =>
  val S3DummyAccessKey = "TESTKEY"
  val S3DummySecretKey = "TESTSECRET"
  val S3DummyDomain = "s3minio.alpakka"

  override lazy val container: MinioContainer = new MinioContainer(S3DummyAccessKey, S3DummySecretKey, S3DummyDomain)

  lazy val s3Settings: S3Settings =
    S3Settings()
      .withEndpointUrl(container.getHostAddress)
      .withCredentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create(S3DummyAccessKey, S3DummySecretKey)))
}
