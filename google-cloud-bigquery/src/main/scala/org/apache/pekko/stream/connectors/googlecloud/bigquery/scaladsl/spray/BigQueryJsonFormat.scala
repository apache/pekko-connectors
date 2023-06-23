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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray

import spray.json.{ JsonFormat, JsonReader, JsonWriter, RootJsonWriter }

import scala.annotation.implicitNotFound

/**
 * A special JsonReader capable of reading a cell from a BigQuery table.
 */
@implicitNotFound(msg = "Cannot find BigQueryJsonReader or BigQueryJsonFormat type class for ${T}")
trait BigQueryJsonReader[T] extends JsonReader[T]

/**
 * A special JsonWriter capable of writing a cell to a BigQuery table.
 */
@implicitNotFound(msg = "Cannot find BigQueryJsonWriter or BigQueryJsonFormat type class for ${T}")
trait BigQueryJsonWriter[T] extends JsonWriter[T]

/**
 * A special JsonFormat signaling that the format reads and writes cells of BigQuery tables.
 */
trait BigQueryJsonFormat[T] extends JsonFormat[T] with BigQueryJsonReader[T] with BigQueryJsonWriter[T]

/**
 * A special JsonReader capable of reading a row from a BigQuery table.
 */
@implicitNotFound(msg = "Cannot find BigQueryRootJsonReader or BigQueryRootJsonFormat type class for ${T}")
trait BigQueryRootJsonReader[T] extends BigQueryJsonReader[T]

/**
 * A special JsonWriter capable of writing a row to a BigQuery table.
 */
@implicitNotFound(msg = "Cannot find BigQueryRootJsonWriter or BigQueryRootJsonFormat type class for ${T}")
trait BigQueryRootJsonWriter[T] extends BigQueryJsonWriter[T] with RootJsonWriter[T]

/**
 * A special JsonFormat signaling that the format produces a row of BigQuery table.
 */
trait BigQueryRootJsonFormat[T]
    extends BigQueryJsonFormat[T]
    with BigQueryRootJsonReader[T]
    with BigQueryRootJsonWriter[T]
