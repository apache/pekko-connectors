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

package org.apache.pekko.stream.connectors.cassandra.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.javadsl.Source
import com.datastax.oss.driver.api.core.cql.{ Row, Statement }

import scala.annotation.varargs

/**
 * Java API.
 */
object CassandraSource {

  /**
   * Prepare, bind and execute a select statement in one go.
   *
   * See <a href="https://docs.datastax.com/en/dse/6.7/cql/cql/cql_using/queriesTOC.html">Querying data</a>.
   */
  @varargs
  def create(session: CassandraSession, cqlStatement: String, bindValues: AnyRef*): Source[Row, NotUsed] =
    session.select(cqlStatement, bindValues: _*)

  /**
   * Create a [[pekko.stream.javadsl.Source Source]] from a given statement.
   *
   * See <a href="https://docs.datastax.com/en/dse/6.7/cql/cql/cql_using/queriesTOC.html">Querying data</a>.
   */
  def create(session: CassandraSession, stmt: Statement[_]): Source[Row, NotUsed] =
    session.select(stmt)

  /**
   * Create a [[pekko.stream.javadsl.Source Source]] from a given statement.
   *
   * See <a href="https://docs.datastax.com/en/dse/6.7/cql/cql/cql_using/queriesTOC.html">Querying data</a>.
   */
  def fromCompletionStage(session: CassandraSession, stmt: CompletionStage[Statement[_]]): Source[Row, NotUsed] =
    session.select(stmt)

}
