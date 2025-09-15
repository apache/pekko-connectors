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

package org.apache.pekko.stream.connectors.slick.javadsl

import java.sql.Connection
import java.sql.PreparedStatement
import java.util.concurrent.CompletionStage
import java.util.function.{ Function => JFunction }
import java.util.function.{ BiFunction => JBiFunction }

import org.apache.pekko
import pekko.Done
import pekko.NotUsed
import pekko.japi.function.Function2
import pekko.stream.connectors.slick.scaladsl.{ Slick => ScalaSlick }
import pekko.stream.javadsl._
import slick.dbio.DBIO
import slick.jdbc.{ GetResult, SQLActionBuilder, SetParameter, SimpleJdbcAction }

import scala.concurrent.ExecutionContext
import scala.jdk.FunctionConverters._
import scala.jdk.FutureConverters._

object Slick {

  /**
   * Java API: creates a Source that performs the specified query against
   *           the specified Slick database and streams the results through
   *           the specified mapper function to turn database each row
   *           element into an instance of T.
   *
   * @param session The database session to use.
   * @param query The query string to execute. There is currently no Java
   *              DSL support for parameter substitution so you will have
   *              to build the full query statement before passing it in.
   * @param mapper A function that takes an individual result row and
   *               transforms it to an instance of T.
   */
  def source[T](
      session: SlickSession,
      query: String,
      mapper: JFunction[SlickRow, T]): Source[T, NotUsed] = {

    val streamingAction = SQLActionBuilder(query, SetParameter.SetUnit).as[T](toSlick(mapper))

    ScalaSlick
      .source[T](streamingAction)(session)
      .asJava
  }

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def flow[T](
      session: SlickSession,
      toStatement: JFunction[T, String] // TODO: or use the pekko japi Function2 interface?
  ): Flow[T, java.lang.Integer, NotUsed] =
    flow(session, 1, toStatement)

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def flow[T](
      session: SlickSession,
      toStatement: Function2[T, Connection, PreparedStatement]): Flow[T, java.lang.Integer, NotUsed] =
    flow(session, 1, toStatement)

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def flow[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: JFunction[T, String]): Flow[T, java.lang.Integer, NotUsed] =
    ScalaSlick
      .flow[T](parallelism, toDBIO(toStatement))(session)
      .map(Int.box)
      .asJava

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def flow[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: Function2[T, Connection, PreparedStatement]): Flow[T, java.lang.Integer, NotUsed] =
    ScalaSlick
      .flow[T](parallelism, toDBIO(toStatement))(session)
      .map(Int.box)
      .asJava

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, then executes
   *           those statements against the specified Slick database
   *           and allows to combine the statement result and element into a result type R.
   *
   * @param session The database session to use.
   * @param executionContext ExecutionContext used to run mapper function in.
   *                         E.g. the dispatcher of the ActorSystem.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   */
  def flowWithPassThrough[T, R](
      session: SlickSession,
      executionContext: ExecutionContext,
      toStatement: JFunction[T, String],
      mapper: JBiFunction[T, java.lang.Integer, R]): Flow[T, R, NotUsed] =
    flowWithPassThrough(session, executionContext, 1, toStatement, mapper)

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, then executes
   *           those statements against the specified Slick database
   *           and allows to combine the statement result and element into a result type R.
   *
   * @param session The database session to use.
   * @param executionContext ExecutionContext used to run mapper function in.
   *                         E.g. the dispatcher of the ActorSystem.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   */
  def flowWithPassThrough[T, R](
      session: SlickSession,
      executionContext: ExecutionContext,
      toStatement: Function2[T, Connection, PreparedStatement],
      mapper: Function2[T, java.lang.Integer, R]): Flow[T, R, NotUsed] =
    flowWithPassThrough(session, executionContext, 1, toStatement, mapper)

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, then executes
   *           those statements against the specified Slick database
   *           and allows to combine the statement result and element into a result type R.
   *
   * @param session The database session to use.
   * @param executionContext ExecutionContext used to run mapper function in.
   *                         E.g. the dispatcher of the ActorSystem.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   */
  def flowWithPassThrough[T, R](
      session: SlickSession,
      executionContext: ExecutionContext,
      parallelism: Int,
      toStatement: JFunction[T, String],
      mapper: JBiFunction[T, java.lang.Integer, R]): Flow[T, R, NotUsed] =
    ScalaSlick
      .flowWithPassThrough[T, R](parallelism,
        (t: T) => {
          toDBIO(toStatement)
            .apply(t)
            .map(count => mapper.apply(t, count))(executionContext)
        })(session)
      .asJava

  /**
   * Java API: creates a Flow that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, then executes
   *           those statements against the specified Slick database
   *           and allows to combine the statement result and element into a result type R.
   *
   * @param session The database session to use.
   * @param executionContext ExecutionContext used to run mapper function in.
   *                         E.g. the dispatcher of the ActorSystem.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   */
  def flowWithPassThrough[T, R](
      session: SlickSession,
      executionContext: ExecutionContext,
      parallelism: Int,
      toStatement: Function2[T, Connection, PreparedStatement],
      mapper: Function2[T, java.lang.Integer, R]): Flow[T, R, NotUsed] =
    ScalaSlick
      .flowWithPassThrough[T, R](parallelism,
        (t: T) => {
          toDBIO(toStatement)
            .apply(t)
            .map(count => mapper.apply(t, count))(executionContext)
        })(session)
      .asJava

  /**
   * Java API: creates a Sink that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def sink[T](
      session: SlickSession,
      toStatement: JFunction[T, String] // TODO: or use the pekko japi Function2 interface?
  ): Sink[T, CompletionStage[Done]] =
    sink(session, 1, toStatement)

  /**
   * Java API: creates a Sink that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def sink[T](
      session: SlickSession,
      toStatement: Function2[T, Connection, PreparedStatement]): Sink[T, CompletionStage[Done]] =
    sink(session, 1, toStatement)

  /**
   * Java API: creates a Sink that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def sink[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: JFunction[T, String]): Sink[T, CompletionStage[Done]] =
    ScalaSlick
      .sink[T](parallelism, toDBIO(toStatement))(session)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Java API: creates a Sink that takes a stream of elements of
   *           type T, transforms each element to a SQL statement
   *           using the specified function, and then executes
   *           those statements against the specified Slick database.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   */
  def sink[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: Function2[T, Connection, PreparedStatement]): Sink[T, CompletionStage[Done]] =
    ScalaSlick
      .sink[T](parallelism, toDBIO(toStatement))(session)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Java API: creates a Sink that takes a stream of complete SQL
   *           statements (e.g. a stream of Strings) to execute
   *           against the specified Slick database.
   *
   * @param session The database session to use.
   */
  def sink(
      session: SlickSession): Sink[String, CompletionStage[Done]] =
    sink[String](session, 1, JFunction.identity[String]())

  /**
   * Java API: creates a Sink that takes a stream of complete SQL
   *           statements (e.g. a stream of Strings) to execute
   *           against the specified Slick database.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   */
  def sink(
      session: SlickSession,
      parallelism: Int): Sink[String, CompletionStage[Done]] =
    sink[String](session, parallelism, JFunction.identity[String]())

  private def toSlick[T](mapper: JFunction[SlickRow, T]): GetResult[T] =
    GetResult(pr => mapper(new SlickRow(pr)))

  private def toDBIO[T](javaDml: JFunction[T, String]): T => DBIO[Int] = { t =>
    SQLActionBuilder(javaDml.asScala(t), SetParameter.SetUnit).asUpdate
  }

  private def toDBIO[T](javaDml: Function2[T, Connection, PreparedStatement]): T => DBIO[Int] = { t =>
    SimpleJdbcAction { ctx =>
      javaDml(t, ctx.connection).executeUpdate()
    }
  }
}
