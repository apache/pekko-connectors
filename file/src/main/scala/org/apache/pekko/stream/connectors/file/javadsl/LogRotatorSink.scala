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

package org.apache.pekko.stream.connectors.file.javadsl

import java.nio.file.{ OpenOption, Path }
import java.util.Optional
import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.Done
import pekko.stream.javadsl
import pekko.stream.scaladsl
import pekko.util.ByteString
import pekko.japi.function

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.jdk.OptionConverters._

/**
 * Java API.
 */
object LogRotatorSink {

  /**
   * Sink directing the incoming `ByteString`s to new files whenever `triggerGenerator` returns a value.
   *
   * @param triggerGeneratorCreator creates a function that triggers rotation by returning a value
   */
  def createFromFunction(
      triggerGeneratorCreator: function.Creator[function.Function[ByteString, Optional[Path]]])
      : javadsl.Sink[ByteString, CompletionStage[Done]] = {
    val logRotatorSink = new scaladsl.SinkToCompletionStage[ByteString, Done](pekko.stream.connectors.file.scaladsl
      .LogRotatorSink(asScala(triggerGeneratorCreator)))
    new javadsl.Sink(logRotatorSink.toCompletionStage())
  }

  /**
   * Sink directing the incoming `ByteString`s to new files whenever `triggerGenerator` returns a value.
   *
   * @param triggerGeneratorCreator creates a function that triggers rotation by returning a value
   * @param fileOpenOptions file options for file creation
   */
  def createFromFunctionAndOptions(
      triggerGeneratorCreator: function.Creator[function.Function[ByteString, Optional[Path]]],
      fileOpenOptions: java.util.Set[? <: OpenOption]): javadsl.Sink[ByteString, CompletionStage[Done]] = {

    val logRotatorSink = new scaladsl.SinkToCompletionStage[ByteString, Done](pekko.stream.connectors.file.scaladsl
      .LogRotatorSink(asScala(triggerGeneratorCreator), fileOpenOptions.asScala.toSet[OpenOption]))
    new javadsl.Sink(logRotatorSink.toCompletionStage())
  }

  /**
   * Sink directing the incoming `ByteString`s to a new `Sink` created by `sinkFactory` whenever `triggerGenerator` returns a value.
   *
   * @param triggerGeneratorCreator creates a function that triggers rotation by returning a value
   * @param sinkFactory creates sinks for `ByteString`s from the value returned by `triggerGenerator`
   * @tparam C criterion type (for files a `Path`)
   * @tparam R result type in materialized futures of `sinkFactory`
   */
  def withSinkFactory[C, R](
      triggerGeneratorCreator: function.Creator[function.Function[ByteString, Optional[C]]],
      sinkFactory: function.Function[C, javadsl.Sink[ByteString, CompletionStage[R]]])
      : javadsl.Sink[ByteString, CompletionStage[Done]] = {
    val t: C => scaladsl.Sink[ByteString, Future[R]] = path =>
      sinkFactory.apply(path).asScala.mapMaterializedValue(_.asScala)
    val logRotatorSink =
      new scaladsl.SinkToCompletionStage[ByteString, Done](pekko.stream.connectors.file.scaladsl.LogRotatorSink
        .withSinkFactory(asScala[C](triggerGeneratorCreator), t))
    new javadsl.Sink(logRotatorSink.toCompletionStage())
  }

  /**
   * Sink directing the incoming `T`s to a new `Sink` created by `sinkFactory` whenever `triggerGenerator` returns a value.
   *
   * @param triggerGeneratorCreator creates a function that triggers rotation by returning a value
   * @param sinkFactory creates sinks for `T`s from the value returned by `triggerGenerator`
   * @tparam T stream and sink data type
   * @tparam C criterion type (for files a `Path`)
   * @tparam R result type in materialized futures of `sinkFactory`
   * @since 2.0.0
   */
  def withTypedSinkFactory[T, C, R](
      triggerGeneratorCreator: function.Creator[function.Function[T, Optional[C]]],
      sinkFactory: function.Function[C, javadsl.Sink[T, CompletionStage[R]]])
      : javadsl.Sink[T, CompletionStage[Done]] = {
    val t: C => scaladsl.Sink[T, Future[R]] = criterion =>
      sinkFactory.apply(criterion).asScala.mapMaterializedValue(_.asScala)
    val logRotatorSink =
      new scaladsl.SinkToCompletionStage[T, Done](pekko.stream.connectors.file.scaladsl.LogRotatorSink
        .withTypedSinkFactory(asScalaTyped[T, C](triggerGeneratorCreator), t))
    new javadsl.Sink(logRotatorSink.toCompletionStage())
  }

  private def asScala[C](
      f: function.Creator[function.Function[ByteString, Optional[C]]]): () => ByteString => Option[C] =
    asScalaTyped[ByteString, C](f)

  private def asScalaTyped[T, C](
      f: function.Creator[function.Function[T, Optional[C]]]): () => T => Option[C] = () => {
    val fun = f.create()
    elem => fun(elem).toScala
  }

}
