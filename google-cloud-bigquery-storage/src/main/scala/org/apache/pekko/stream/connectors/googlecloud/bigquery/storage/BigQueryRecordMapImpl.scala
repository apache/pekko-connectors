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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage

import org.apache.avro.generic.GenericRecord

trait BigQueryRecord {

  def get(column: String): Option[Object]

}

object BigQueryRecord {

  def fromMap(map: Map[String, Object]): BigQueryRecord = BigQueryRecordMapImpl(map)

  def fromAvro(record: GenericRecord): BigQueryRecord = BigQueryRecordAvroImpl(record)

}

final case class BigQueryRecordAvroImpl(record: GenericRecord) extends BigQueryRecord {

  override def get(column: String): Option[Object] = Option(record.get(column))

  override def equals(that: Any): Boolean = that match {
    case BigQueryRecordAvroImpl(thatRecord) => thatRecord.equals(record)
    case _                                  => false
  }

  override def hashCode(): Int = record.hashCode()

}

final case class BigQueryRecordMapImpl(map: Map[String, Object]) extends BigQueryRecord {

  override def get(column: String): Option[Object] = map.get(column)

  override def equals(that: Any): Boolean = that match {
    case BigQueryRecordMapImpl(thatMap) => thatMap.equals(map)
    case _                              => false
  }

  override def hashCode(): Int = map.hashCode()

}
