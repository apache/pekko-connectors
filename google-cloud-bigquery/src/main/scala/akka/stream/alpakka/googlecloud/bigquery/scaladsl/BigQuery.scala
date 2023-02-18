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

package akka.stream.alpakka.googlecloud.bigquery.scaladsl

import akka.annotation.ApiMayChange

/**
 * Scala API to interface with BigQuery.
 */
@ApiMayChange(issue = "https://github.com/akka/alpakka/pull/2548")
object BigQuery
    extends BigQueryRest
    with BigQueryDatasets
    with BigQueryJobs
    with BigQueryQueries
    with BigQueryTables
    with BigQueryTableData
