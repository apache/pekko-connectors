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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.Attributes.Attribute

/**
 * Akka Stream attributes that are used when materializing BigQuery Storage stream blueprints.
 */
object BigQueryStorageAttributes {

  /**
   * gRPC client to use for the stream
   */
  def reader(client: GrpcBigQueryStorageReader): Attributes = Attributes(new BigQueryStorageReader(client))

  final class BigQueryStorageReader @InternalApi private[BigQueryStorageAttributes] (
      val client: GrpcBigQueryStorageReader) extends Attribute
}
