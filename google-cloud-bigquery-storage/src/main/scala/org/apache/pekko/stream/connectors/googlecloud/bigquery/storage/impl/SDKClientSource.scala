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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.impl

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import com.google.cloud.bigquery.storage.v1.storage.{ BigQueryReadClient, ReadRowsRequest, ReadRowsResponse }
import com.google.cloud.bigquery.storage.v1.stream.ReadSession

object SDKClientSource {

  private val RequestParamsHeader = "x-goog-request-params"

  def read(client: BigQueryReadClient, readSession: ReadSession): Seq[Source[ReadRowsResponse.Rows, NotUsed]] = {
    readSession.streams
      .map(stream => {
        client
          .readRows()
          .addHeader(RequestParamsHeader, s"read_stream=${stream.name}")
          .invoke(ReadRowsRequest(stream.name))
          .map(_.rows)
      })
  }

}
