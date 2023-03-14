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

package org.apache.pekko.stream.connectors.googlecloud.storage.impl

import org.apache.pekko.stream.connectors.googlecloud.storage.StorageObject
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.google.scaladsl.Paginated

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
