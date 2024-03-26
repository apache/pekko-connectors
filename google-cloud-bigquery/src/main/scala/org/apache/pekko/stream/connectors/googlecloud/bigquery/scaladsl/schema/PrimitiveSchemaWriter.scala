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

import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.{
  TableFieldSchema,
  TableFieldSchemaMode,
  TableFieldSchemaType
}

private[schema] final class PrimitiveSchemaWriter[T](`type`: TableFieldSchemaType) extends SchemaWriter[T] {

  override def write(name: String, mode: TableFieldSchemaMode): TableFieldSchema =
    TableFieldSchema(name, `type`, Some(mode), None)

}
