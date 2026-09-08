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

package org.apache.pekko.stream.connectors.hbase.impl

import java.io.Closeable

import org.apache.pekko.stream.stage.StageLogging
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{
  ColumnFamilyDescriptorBuilder,
  Connection,
  ConnectionFactory,
  Table,
  TableDescriptorBuilder
}
import org.apache.hadoop.conf.Configuration

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }

private[impl] trait HBaseCapabilities { this: StageLogging =>

  def twr[A <: Closeable, B](resource: A)(doWork: A => B): Try[B] =
    try {
      Success(doWork(resource))
    } catch {
      case e: Exception => Failure(e)
    } finally {
      try {
        if (resource != null) {
          resource.close()
        }
      } catch {
        case e: Exception => log.error(e, e.getMessage) // should be logged
      }
    }

  /**
   * Connect to hbase cluster.
   *
   * @param conf
   * @param timeout in second
   * @return
   */
  def connect(conf: Configuration, timeout: Int = 10) =
    Await.result(Future(ConnectionFactory.createConnection(conf)), timeout.seconds)

  private[impl] def getOrCreateTable(tableName: TableName, columnFamilies: Seq[String])(
      implicit connection: Connection): Try[Table] = twr(connection.getAdmin) { admin =>
    val table =
      if (admin.isTableAvailable(tableName))
        connection.getTable(tableName)
      else {
        val tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(tableName)
        columnFamilies.foreach { cf =>
          tableDescriptorBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.of(cf))
        }
        admin.createTable(tableDescriptorBuilder.build())
        log.info("Table {} created with cfs: {}.", tableName, columnFamilies)
        connection.getTable(tableName)
      }
    table
  }

}
