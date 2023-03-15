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

package org.apache.pekko.stream.connectors.cassandra.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.scaladsl.Source
import com.datastax.oss.driver.api.core.cql.{ Row, Statement }

import scala.concurrent.Future

/**
 * Scala API.
 */
object CassandraSource {

  /**
   * Prepare, bind and execute a select statement in one go.
   *
   * See <a href="https://docs.datastax.com/en/dse/6.7/cql/cql/cql_using/queriesTOC.html">Querying data</a>.
   */
  def apply(cqlStatement: String, bindValues: AnyRef*)(implicit session: CassandraSession): Source[Row, NotUsed] =
    session.select(cqlStatement, bindValues: _*)

  /**
   * Create a [[pekko.stream.scaladsl.Source Source]] from a given statement.
   *
   * See <a href="https://docs.datastax.com/en/dse/6.7/cql/cql/cql_using/queriesTOC.html">Querying data</a>.
   */
  def apply(stmt: Statement[_])(implicit session: CassandraSession): Source[Row, NotUsed] =
    session.select(stmt)

  /**
   * Create a [[pekko.stream.scaladsl.Source Source]] from a given statement.
   *
   * See <a href="https://docs.datastax.com/en/dse/6.7/cql/cql/cql_using/queriesTOC.html">Querying data</a>.
   */
  def fromFuture(stmt: Future[Statement[_]])(implicit session: CassandraSession): Source[Row, NotUsed] =
    session.select(stmt)

}
