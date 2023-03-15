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

import org.apache.pekko
import pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaType.Record
import pekko.stream.connectors.googlecloud.bigquery.model.{ TableFieldSchema, TableFieldSchemaMode, TableSchema }
import spray.json.{ AdditionalFormats, ProductFormats, StandardFormats }

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

/**
 * Provides the helpers for constructing custom SchemaWriter implementations for types implementing the Product trait
 * (especially case classes)
 */
trait ProductSchemas extends ProductSchemasInstances { this: StandardSchemas =>

  protected def extractFieldNames(tag: ClassTag[_]): Array[String] =
    ProductSchemasSupport.extractFieldNames(tag)

}

private[schema] final class ProductSchemaWriter[T <: Product](fieldSchemas: Seq[TableFieldSchema])
    extends TableSchemaWriter[T] {

  override def write: TableSchema = TableSchema(fieldSchemas)

  override def write(name: String, mode: TableFieldSchemaMode): TableFieldSchema =
    TableFieldSchema(name, Record, Some(mode), Some(fieldSchemas))

}

private object ProductSchemasSupport extends StandardFormats with ProductFormats with AdditionalFormats {
  override def extractFieldNames(tag: ClassTag[_]): Array[String] = super.extractFieldNames(tag)
}
