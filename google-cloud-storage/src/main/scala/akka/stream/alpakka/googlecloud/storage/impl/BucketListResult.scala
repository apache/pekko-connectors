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

package akka.stream.alpakka.googlecloud.storage.impl

import akka.stream.alpakka.googlecloud.storage.StorageObject
import akka.annotation.InternalApi
import akka.stream.alpakka.google.scaladsl.Paginated

@InternalApi
private[impl] final case class BucketListResult(
    kind: String,
    nextPageToken: Option[String],
    prefixes: Option[List[String]],
    items: List[StorageObject]) {
  def merge(other: BucketListResult): BucketListResult =
    copy(nextPageToken = None, items = this.items ++ other.items,
      prefixes = for {
        source <- this.prefixes
        other <- other.prefixes
      } yield source ++ other)
}

@InternalApi
private[impl] object BucketListResult {
  implicit val paginated: Paginated[BucketListResult] = _.nextPageToken
}
