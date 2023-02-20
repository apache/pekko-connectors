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

package akka.stream.alpakka.googlecloud.bigquery.scaladsl.spray

import spray.json.{ AdditionalFormats, ProductFormats }

/**
 * Provides all the predefined BigQueryJsonFormats for rows and cells in BigQuery tables.
 */
trait BigQueryJsonProtocol
    extends BigQueryBasicFormats
    with BigQueryStandardFormats
    with BigQueryCollectionFormats
    with ProductFormats
    with BigQueryProductFormats
    with AdditionalFormats
    with BigQueryJavaTimeFormats

object BigQueryJsonProtocol extends BigQueryJsonProtocol
