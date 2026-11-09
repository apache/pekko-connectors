/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.slick.javadsl

import java.sql.{ Connection, PreparedStatement }
import java.util.concurrent.{ CompletionStage, Executor }
import java.util.function.{ BiFunction => JBiFunction, Function => JFunction }

import org.apache.pekko
import pekko.NotUsed
import pekko.japi.function.Function2
import pekko.stream.connectors.slick.scaladsl.{ SlickWithTryResult => ScalaSlickWithTryResult }
import pekko.stream.javadsl.{ Flow, Keep, Sink }

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Java API: methods for interacting with relational databases using Slick and pekko-stream, which emit
 * a `scala.util.Try` for each statement instead of failing the stream.
 *
 * @since 2.0.0
 */
object SlickWithTryResult {

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the statement result, or a `Failure` if there was an exception
   *           during the execution, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def flowTry[T](
      session: SlickSession,
      toStatement: JFunction[T, String]): Flow[T, Try[java.lang.Integer], NotUsed] =
    flowTry(session, 1, toStatement)

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the statement result, or a `Failure` if there was an exception
   *           during the execution, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def flowTry[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: JFunction[T, String]): Flow[T, Try[java.lang.Integer], NotUsed] =
    ScalaSlickWithTryResult
      .flowTry[T](parallelism, Slick.toDBIO(toStatement))(session)
      .map(_.map(Int.box))
      .asJava

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the statement result, or a `Failure` if there was an exception
   *           during the execution, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def flowTry[T](
      session: SlickSession,
      toStatement: Function2[T, Connection, PreparedStatement]): Flow[T, Try[java.lang.Integer], NotUsed] =
    flowTry(session, 1, toStatement)

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the statement result, or a `Failure` if there was an exception
   *           during the execution, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def flowTry[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: Function2[T, Connection, PreparedStatement]): Flow[T, Try[java.lang.Integer], NotUsed] =
    ScalaSlickWithTryResult
      .flowTry[T](parallelism, Slick.toDBIO(toStatement))(session)
      .map(_.map(Int.box))
      .asJava

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the mapper's result, or a `Failure` if there was an exception
   *           during the execution or in the mapper, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param executor Executor used to run mapper function in.
   *                 E.g. the dispatcher of the ActorSystem.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   * @since 2.0.0
   */
  def flowTryWithPassThrough[T, R](
      session: SlickSession,
      executor: Executor,
      toStatement: JFunction[T, String],
      mapper: JBiFunction[T, java.lang.Integer, R]): Flow[T, Try[R], NotUsed] =
    flowTryWithPassThrough(session, executor, 1, toStatement, mapper)

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the mapper's result, or a `Failure` if there was an exception
   *           during the execution or in the mapper, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param executor Executor used to run mapper function in.
   *                 E.g. the dispatcher of the ActorSystem.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   * @since 2.0.0
   */
  def flowTryWithPassThrough[T, R](
      session: SlickSession,
      executor: Executor,
      parallelism: Int,
      toStatement: JFunction[T, String],
      mapper: JBiFunction[T, java.lang.Integer, R]): Flow[T, Try[R], NotUsed] = {
    val executionContext = ExecutionContext.fromExecutor(executor)
    ScalaSlickWithTryResult
      .flowTryWithPassThrough[T, R](parallelism,
        (t: T) =>
          Slick
            .toDBIO(toStatement)
            .apply(t)
            .map(count => mapper.apply(t, count))(executionContext))(session)
      .asJava
  }

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the mapper's result, or a `Failure` if there was an exception
   *           during the execution or in the mapper, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param executor Executor used to run mapper function in.
   *                 E.g. the dispatcher of the ActorSystem.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   * @since 2.0.0
   */
  def flowTryWithPassThrough[T, R](
      session: SlickSession,
      executor: Executor,
      toStatement: Function2[T, Connection, PreparedStatement],
      mapper: Function2[T, java.lang.Integer, R]): Flow[T, Try[R], NotUsed] =
    flowTryWithPassThrough(session, executor, 1, toStatement, mapper)

  /**
   * Java API: creates a Flow that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database. It emits a `Success` with the mapper's result, or a `Failure` if there was an exception
   *           during the execution or in the mapper, and the stream carries on with the next element.
   *
   * @param session The database session to use.
   * @param executor Executor used to run mapper function in.
   *                 E.g. the dispatcher of the ActorSystem.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @param mapper A function to create a result from the incoming element T
   *               and the database statement result.
   * @since 2.0.0
   */
  def flowTryWithPassThrough[T, R](
      session: SlickSession,
      executor: Executor,
      parallelism: Int,
      toStatement: Function2[T, Connection, PreparedStatement],
      mapper: Function2[T, java.lang.Integer, R]): Flow[T, Try[R], NotUsed] = {
    val executionContext = ExecutionContext.fromExecutor(executor)
    ScalaSlickWithTryResult
      .flowTryWithPassThrough[T, R](parallelism,
        (t: T) =>
          Slick
            .toDBIO(toStatement)
            .apply(t)
            .map(count => mapper.apply(t, count))(executionContext))(session)
      .asJava
  }

  /**
   * Java API: creates a Sink that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database.
   *
   *           The materialized `CompletionStage` contains the `Try` of the last element only, so failures of earlier
   *           elements are not reported. It fails if the stream is empty.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def sinkTry[T](
      session: SlickSession,
      toStatement: JFunction[T, String]): Sink[T, CompletionStage[Try[java.lang.Integer]]] =
    sinkTry(session, 1, toStatement)

  /**
   * Java API: creates a Sink that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database.
   *
   *           The materialized `CompletionStage` contains the `Try` of the last element only, so failures of earlier
   *           elements are not reported. It fails if the stream is empty.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def sinkTry[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: JFunction[T, String]): Sink[T, CompletionStage[Try[java.lang.Integer]]] =
    flowTry(session, parallelism, toStatement)
      .toMat(Sink.last[Try[java.lang.Integer]](), Keep.right[NotUsed, CompletionStage[Try[java.lang.Integer]]])

  /**
   * Java API: creates a Sink that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database.
   *
   *           The materialized `CompletionStage` contains the `Try` of the last element only, so failures of earlier
   *           elements are not reported. It fails if the stream is empty.
   *
   * @param session The database session to use.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def sinkTry[T](
      session: SlickSession,
      toStatement: Function2[T, Connection, PreparedStatement]): Sink[T, CompletionStage[Try[java.lang.Integer]]] =
    sinkTry(session, 1, toStatement)

  /**
   * Java API: creates a Sink that takes a stream of elements of type T, transforms each element to a SQL statement
   *           using the specified function, and tries executing those statements against the specified Slick
   *           database.
   *
   *           The materialized `CompletionStage` contains the `Try` of the last element only, so failures of earlier
   *           elements are not reported. It fails if the stream is empty.
   *
   * @param session The database session to use.
   * @param parallelism How many parallel asynchronous streams should be
   *                    used to send statements to the database. Use a
   *                    value of 1 for sequential execution.
   * @param toStatement A function that creates the SQL statement to
   *                    execute from the current element. Any DML or
   *                    DDL statement is acceptable.
   * @since 2.0.0
   */
  def sinkTry[T](
      session: SlickSession,
      parallelism: Int,
      toStatement: Function2[T, Connection, PreparedStatement]): Sink[T, CompletionStage[Try[java.lang.Integer]]] =
    flowTry(session, parallelism, toStatement)
      .toMat(Sink.last[Try[java.lang.Integer]](), Keep.right[NotUsed, CompletionStage[Try[java.lang.Integer]]])
}
