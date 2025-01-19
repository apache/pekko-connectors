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
package org.apache.pekko.stream.connectors.slick.scaladsl

import scala.concurrent.Future
import scala.util.Try
import org.apache.pekko
import pekko.NotUsed
import pekko.stream.scaladsl.Flow
import pekko.stream.scaladsl.Keep
import pekko.stream.scaladsl.Sink
import slick.dbio.DBIO

/**
 * Methods for interacting with relational databases using Slick and pekko-stream.
 */
object SlickWithTryResult {

  /**
   * Scala API: creates a Flow that takes a stream of elements of
   *            type T, transforms each element to a SQL statement
   *            using the specified function, and then try executing
   *            those statements against the specified Slick database.
   *            It return Success[Int] or Failure[Throwable]
   *            if there was an exception during the execution.
   *
   * @param toStatement A function to produce the SQL statement to
   *                    execute based on the current element.
   * @param session The database session to use.
   */
  def flowTry[T](
      toStatement: T => DBIO[Int])(implicit session: SlickSession): Flow[T, Try[Int], NotUsed] = flowTry(1, toStatement)

  /**
   * Scala API: creates a Flow that takes a stream of elements of
   *            type T, transforms each element to a SQL statement
   *            using the specified function, and then executes
   *            those statements against the specified Slick database.
   *            It return Success[Int] or Failure[Throwable]
   *            if there was an exception during the execution.
   *
   * @param toStatement A function to produce the SQL statement to
   *                    execute based on the current element.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param session The database session to use.
   */
  def flowTry[T](
      parallelism: Int,
      toStatement: T => DBIO[Int])(implicit session: SlickSession): Flow[T, Try[Int], NotUsed] =
    flowTryWithPassThrough(parallelism, toStatement)

  /**
   * Scala API: creates a Flow that takes a stream of elements of
   *            type T, transforms each element to a SQL statement
   *            using the specified function, then executes
   *            those statements against the specified Slick database
   *            and returns the statement result type Success[R] or
   *            Failure[Throwable] if there is an exception.
   *
   * @param toStatement A function to produce the SQL statement to
   *                    execute based on the current element.
   * @param session The database session to use.
   */
  def flowTryWithPassThrough[T, R](
      toStatement: T => DBIO[R])(implicit session: SlickSession): Flow[T, Try[R], NotUsed] =
    flowTryWithPassThrough(1, toStatement)

  /**
   * Scala API: creates a Flow that takes a stream of elements of
   *            type T, transforms each element to a SQL statement
   *            using the specified function, then executes
   *            those statements against the specified Slick database
   *            and returns the statement result type Success[R] or
   *            Failure[Throwable] if there is an exception.
   *
   * @param toStatement A function to produce the SQL statement to
   *                    execute based on the current element.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param session The database session to use.
   */
  def flowTryWithPassThrough[T, R](
      parallelism: Int,
      toStatement: T => DBIO[R])(implicit session: SlickSession): Flow[T, Try[R], NotUsed] =
    Flow[T]
      .mapAsync(parallelism) { t =>
        session.db.run(toStatement(t).asTry)
      }

  /**
   * Scala API: creates a Sink that takes a stream of elements of
   *            type T, transforms each element to a SQL statement
   *            using the specified function, and then executes
   *            those statements against the specified Slick database.
   *
   * @param toStatement A function to produce the SQL statement to
   *                    execute based on the current element.
   * @param session The database session to use.
   */
  def sinkTry[T](
      toStatement: T => DBIO[Int])(implicit session: SlickSession): Sink[T, Future[Try[Int]]] =
    flowTry[T](1, toStatement).toMat(Sink.last)(Keep.right)

  /**
   * Scala API: creates a Sink that takes a stream of elements of
   *            type T, transforms each element to a SQL statement
   *            using the specified function, and then executes
   *            those statements against the specified Slick database.
   *
   * @param toStatement A function to produce the SQL statement to
   *                    execute based on the current element.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param session The database session to use.
   */
  def sinkTry[T](
      parallelism: Int,
      toStatement: T => DBIO[Int])(implicit session: SlickSession): Sink[T, Future[Try[Int]]] =
    flowTry[T](parallelism, toStatement).toMat(Sink.last)(Keep.right)
}
