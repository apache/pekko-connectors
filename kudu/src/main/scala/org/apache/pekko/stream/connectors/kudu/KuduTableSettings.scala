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

package org.apache.pekko.stream.connectors.kudu

import org.apache.kudu.client.PartialRow

import scala.jdk.FunctionConverters._

final class KuduTableSettings[T] private (val tableName: String,
    val schema: org.apache.kudu.Schema,
    val createTableOptions: org.apache.kudu.client.CreateTableOptions,
    val converter: T => org.apache.kudu.client.PartialRow) {

  def withTableName(value: String): KuduTableSettings[T] = copy(tableName = value)
  def withSchema(value: org.apache.kudu.Schema): KuduTableSettings[T] = copy(schema = value)
  def withCreateTableOptions(value: org.apache.kudu.client.CreateTableOptions): KuduTableSettings[T] =
    copy(createTableOptions = value)
  def withConverter[A](value: A => org.apache.kudu.client.PartialRow): KuduTableSettings[A] =
    new KuduTableSettings(tableName = tableName,
      schema = schema,
      createTableOptions = createTableOptions,
      converter = value)

  private def copy(
      tableName: String = tableName,
      schema: org.apache.kudu.Schema = schema,
      createTableOptions: org.apache.kudu.client.CreateTableOptions = createTableOptions): KuduTableSettings[T] =
    new KuduTableSettings(tableName = tableName,
      schema = schema,
      createTableOptions = createTableOptions,
      converter = converter)

  override def toString =
    s"""KuduTableSettings(tableName=$tableName,schema=$schema,createTableOptions=$createTableOptions,converter=$converter)"""
}

object KuduTableSettings {

  /** Scala API */
  def apply[T](
      tableName: String,
      schema: org.apache.kudu.Schema,
      createTableOptions: org.apache.kudu.client.CreateTableOptions,
      converter: T => org.apache.kudu.client.PartialRow): KuduTableSettings[T] = new KuduTableSettings(
    tableName,
    schema,
    createTableOptions,
    converter)

  /** Java API */
  def create[T](
      tableName: String,
      schema: org.apache.kudu.Schema,
      createTableOptions: org.apache.kudu.client.CreateTableOptions,
      converter: java.util.function.Function[T, PartialRow]): KuduTableSettings[T] = new KuduTableSettings(
    tableName,
    schema,
    createTableOptions,
    converter.asScala)
}
