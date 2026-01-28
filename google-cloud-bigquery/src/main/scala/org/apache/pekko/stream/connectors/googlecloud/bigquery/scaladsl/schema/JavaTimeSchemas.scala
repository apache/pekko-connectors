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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.schema

import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaType.{
  Date,
  DateTime,
  Time,
  Timestamp
}

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime }

/**
 * Provides BigQuery schemas for [[java.time]] classes.
 */
trait JavaTimeSchemas {
  implicit val localDateSchemaWriter: SchemaWriter[LocalDate] = new PrimitiveSchemaWriter(Date)
  implicit val localTimeSchemaWriter: SchemaWriter[LocalTime] = new PrimitiveSchemaWriter(Time)
  implicit val localDateTimeSchemaWriter: SchemaWriter[LocalDateTime] = new PrimitiveSchemaWriter(DateTime)
  implicit val instantSchemaWriter: SchemaWriter[Instant] = new PrimitiveSchemaWriter[Instant](Timestamp)
}
