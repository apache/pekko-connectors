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

package org.apache.pekko.stream.connectors.s3.headers

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.model.HttpHeader
import org.apache.pekko.http.scaladsl.model.headers.RawHeader

/**
 * Documentation: http://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#canned-acl
 */
final class CannedAcl private (val value: String) {
  @InternalApi private[s3] def header: HttpHeader = RawHeader("x-amz-acl", value)
}

object CannedAcl {
  val AuthenticatedRead = new CannedAcl("authenticated-read")
  val AwsExecRead = new CannedAcl("aws-exec-read")
  val BucketOwnerFullControl = new CannedAcl("bucket-owner-full-control")
  val BucketOwnerRead = new CannedAcl("bucket-owner-read")
  val Private = new CannedAcl("private")
  val PublicRead = new CannedAcl("public-read")
  val PublicReadWrite = new CannedAcl("public-read-write")
}
