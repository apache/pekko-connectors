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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.schema

import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaType.{
  Boolean,
  Float,
  Integer,
  Numeric,
  String
}

/**
 * Provides the BigQuery schemas for the most important Scala types.
 */
trait BasicSchemas {
  implicit val intSchemaWriter: SchemaWriter[Int] = new PrimitiveSchemaWriter(Integer)
  implicit val longSchemaWriter: SchemaWriter[Long] = new PrimitiveSchemaWriter(Integer)
  implicit val floatSchemaWriter: SchemaWriter[Float] = new PrimitiveSchemaWriter(Float)
  implicit val doubleSchemaWriter: SchemaWriter[Double] = new PrimitiveSchemaWriter(Float)
  implicit val byteSchemaWriter: SchemaWriter[Byte] = new PrimitiveSchemaWriter(Integer)
  implicit val shortSchemaWriter: SchemaWriter[Short] = new PrimitiveSchemaWriter(Integer)
  implicit val bigDecimalSchemaWriter: SchemaWriter[BigDecimal] = new PrimitiveSchemaWriter(Numeric)
  implicit val booleanSchemaWriter: SchemaWriter[Boolean] = new PrimitiveSchemaWriter(Boolean)
  implicit val charSchemaWriter: SchemaWriter[Char] = new PrimitiveSchemaWriter(String)
  implicit val stringSchemaWriter: SchemaWriter[String] = new PrimitiveSchemaWriter(String)
}
