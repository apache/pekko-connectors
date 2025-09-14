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

package org.apache.pekko.stream.connectors.cassandra.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.cassandra.CassandraWriteSettings
import pekko.stream.scaladsl.{ Flow, FlowWithContext }
import com.datastax.oss.driver.api.core.cql.{ BatchStatement, BoundStatement, PreparedStatement }

import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._

/**
 * Scala API to create Cassandra flows.
 */
object CassandraFlow {

  /**
   * A flow writing to Cassandra for every stream element.
   * The element to be persisted is emitted unchanged.
   *
   * @param writeSettings settings to configure the write operation
   * @param cqlStatement raw CQL statement
   * @param statementBinder function to bind data from the stream element to the prepared statement
   * @param session implicit Cassandra session from `CassandraSessionRegistry`
   * @tparam T stream element type
   */
  def create[T](
      writeSettings: CassandraWriteSettings,
      cqlStatement: String,
      statementBinder: (T, PreparedStatement) => BoundStatement)(
      implicit session: CassandraSession): Flow[T, T, NotUsed] = {
    Flow
      .lazyFutureFlow { () =>
        val prepare = session.prepare(cqlStatement)
        prepare.map { preparedStatement =>
          Flow[T].mapAsync(writeSettings.parallelism) { element =>
            session
              .executeWrite(statementBinder(element, preparedStatement))
              .map(_ => element)(ExecutionContext.parasitic)
          }
        }(session.ec)
      }
      .mapMaterializedValue(_ => NotUsed)
  }

  /**
   * A flow writing to Cassandra for every stream element, passing context along.
   * The element (to be persisted) and the context are emitted unchanged.
   *
   * @param writeSettings settings to configure the write operation
   * @param cqlStatement raw CQL statement
   * @param statementBinder function to bind data from the stream element to the prepared statement
   * @param session implicit Cassandra session from `CassandraSessionRegistry`
   * @tparam T stream element type
   * @tparam Ctx context type
   */
  def withContext[T, Ctx](
      writeSettings: CassandraWriteSettings,
      cqlStatement: String,
      statementBinder: (T, PreparedStatement) => BoundStatement)(
      implicit session: CassandraSession): FlowWithContext[T, Ctx, T, Ctx, NotUsed] = {
    FlowWithContext.fromTuples {
      Flow
        .lazyFutureFlow { () =>
          val prepare = session.prepare(cqlStatement)
          prepare.map { preparedStatement =>
            Flow[(T, Ctx)].mapAsync(writeSettings.parallelism) {
              case tuple @ (element, _) =>
                session
                  .executeWrite(statementBinder(element, preparedStatement))
                  .map(_ => tuple)(ExecutionContext.parasitic)
            }
          }(session.ec)
        }
        .mapMaterializedValue(_ => NotUsed)
    }
  }

  /**
   * Creates a flow that uses [[com.datastax.oss.driver.api.core.cql.BatchStatement]] and groups the
   * elements internally into batches using the `writeSettings` and per `groupingKey`.
   * Use this when most of the elements in the stream share the same partition key.
   *
   * Cassandra batches that share the same partition key will only
   * resolve to one write internally in Cassandra, boosting write performance.
   *
   * "A LOGGED batch to a single partition will be converted to an UNLOGGED batch as an optimization."
   * ([[https://cassandra.apache.org/doc/latest/cql/dml.html#batch Batch CQL]])
   *
   * Be aware that this stage does NOT preserve the upstream order.
   *
   * @param writeSettings settings to configure the batching and the write operation
   * @param cqlStatement raw CQL statement
   * @param statementBinder function to bind data from the stream element to the prepared statement
   * @param groupingKey groups the elements to go into the same batch
   * @param session implicit Cassandra session from `CassandraSessionRegistry`
   * @tparam T stream element type
   * @tparam K extracted key type for grouping into batches
   */
  def createBatch[T, K](writeSettings: CassandraWriteSettings,
      cqlStatement: String,
      statementBinder: (T, PreparedStatement) => BoundStatement,
      groupingKey: T => K)(implicit session: CassandraSession): Flow[T, T, NotUsed] = {
    Flow
      .lazyFutureFlow { () =>
        val prepareStatement: Future[PreparedStatement] = session.prepare(cqlStatement)
        prepareStatement.map { preparedStatement =>
          Flow[T]
            .groupedWithin(writeSettings.maxBatchSize, writeSettings.maxBatchWait)
            .map(_.groupBy(groupingKey).values.toList)
            .mapConcat(identity)
            .mapAsyncUnordered(writeSettings.parallelism) { list =>
              val boundStatements = list.map(t => statementBinder(t, preparedStatement))
              val batchStatement = BatchStatement.newInstance(writeSettings.batchType).addAll(boundStatements.asJava)
              session.executeWriteBatch(batchStatement).map(_ => list)(ExecutionContext.parasitic)
            }
            .mapConcat(_.toList)
        }(session.ec)
      }
      .mapMaterializedValue(_ => NotUsed)
  }
}
