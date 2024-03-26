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

package org.apache.pekko.stream.connectors.s3.headers

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.http.scaladsl.model.HttpHeader
import pekko.http.scaladsl.model.headers.RawHeader

/**
 * Documentation: http://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html
 */
final class StorageClass private (val storageClass: String) {
  @InternalApi private[s3] def header: HttpHeader = RawHeader("x-amz-storage-class", storageClass)
}

object StorageClass {
  val Standard: StorageClass = new StorageClass("STANDARD")
  val InfrequentAccess: StorageClass = new StorageClass("STANDARD_IA")
  val Glacier: StorageClass = new StorageClass("GLACIER")
  val ReducedRedundancy: StorageClass = new StorageClass("REDUCED_REDUNDANCY")
}
