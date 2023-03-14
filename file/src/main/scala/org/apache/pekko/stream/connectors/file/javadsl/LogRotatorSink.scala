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

package org.apache.pekko.stream.connectors.file.javadsl

import java.nio.file.{ Path, StandardOpenOption }
import java.util.Optional
import java.util.concurrent.CompletionStage

import org.apache.pekko.Done
import org.apache.pekko.stream.javadsl
import org.apache.pekko.stream.scaladsl
import org.apache.pekko.stream.javadsl.Sink
import org.apache.pekko.util.ByteString
import org.apache.pekko.japi.function

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._

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
      : javadsl.Sink[ByteString, CompletionStage[Done]] =
    new Sink(
      org.apache.pekko.stream.connectors.file.scaladsl
        .LogRotatorSink(asScala(triggerGeneratorCreator))
        .toCompletionStage())

  /**
   * Sink directing the incoming `ByteString`s to new files whenever `triggerGenerator` returns a value.
   *
   * @param triggerGeneratorCreator creates a function that triggers rotation by returning a value
   * @param fileOpenOptions file options for file creation
   */
  def createFromFunctionAndOptions(
      triggerGeneratorCreator: function.Creator[function.Function[ByteString, Optional[Path]]],
      fileOpenOptions: java.util.Set[StandardOpenOption]): javadsl.Sink[ByteString, CompletionStage[Done]] =
    new Sink(
      org.apache.pekko.stream.connectors.file.scaladsl
        .LogRotatorSink(asScala(triggerGeneratorCreator), fileOpenOptions.asScala.toSet)
        .toCompletionStage())

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
      sinkFactory: function.Function[C, Sink[ByteString, CompletionStage[R]]])
      : javadsl.Sink[ByteString, CompletionStage[Done]] = {
    val t: C => scaladsl.Sink[ByteString, Future[R]] = path =>
      sinkFactory.apply(path).asScala.mapMaterializedValue(_.toScala)
    new Sink(
      org.apache.pekko.stream.connectors.file.scaladsl.LogRotatorSink
        .withSinkFactory(asScala[C](triggerGeneratorCreator), t)
        .toCompletionStage())
  }

  private def asScala[C](
      f: function.Creator[function.Function[ByteString, Optional[C]]]): () => ByteString => Option[C] = () => {
    val fun = f.create()
    elem => fun(elem).asScala
  }

}
