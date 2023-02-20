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

package akka.stream.alpakka.s3.headers

import akka.annotation.InternalApi
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader

/**
 * Documentation: http://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html
 */
final class StorageClass private (val storageClass: String) {
  @InternalApi private[s3] def header: HttpHeader = RawHeader("x-amz-storage-class", storageClass)
}

object StorageClass {
  val Standard = new StorageClass("STANDARD")
  val InfrequentAccess = new StorageClass("STANDARD_IA")
  val Glacier = new StorageClass("GLACIER")
  val ReducedRedundancy = new StorageClass("REDUCED_REDUNDANCY")
}
