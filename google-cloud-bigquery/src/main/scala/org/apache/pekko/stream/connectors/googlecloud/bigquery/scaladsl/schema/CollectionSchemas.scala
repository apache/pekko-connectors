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

import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaMode.Repeated
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaType.Bytes
import org.apache.pekko.util.ByteString

import scala.collection.Iterable

trait CollectionSchemas extends LowPriorityCollectionSchemas {

  /**
   * Supplies the SchemaWriter for Arrays.
   */
  implicit def arraySchemaWriter[T](implicit writer: SchemaWriter[T]): SchemaWriter[Array[T]] = { (name, mode) =>
    require(mode != Repeated, "A collection cannot be nested inside another collection.")
    writer.write(name, Repeated)
  }

  // Placed here to establish priority over iterableSchemaWriter[Byte]
  implicit val byteStringSchemaWriter: SchemaWriter[ByteString] = new PrimitiveSchemaWriter(Bytes)
}

private[schema] trait LowPriorityCollectionSchemas {

  /**
   * Supplies the SchemaWriter for Iterables.
   */
  implicit def iterableSchemaWriter[T](implicit writer: SchemaWriter[T]): SchemaWriter[Iterable[T]] = { (name, mode) =>
    require(mode != Repeated, "A collection cannot be nested inside another collection.")
    writer.write(name, Repeated)
  }

}
