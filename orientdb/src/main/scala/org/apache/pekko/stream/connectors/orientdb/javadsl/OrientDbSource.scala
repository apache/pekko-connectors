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

package org.apache.pekko.stream.connectors.orientdb.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.orientdb._
import pekko.stream.connectors.orientdb.impl.OrientDbSourceStage
import pekko.stream.javadsl.Source
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Java API.
 */
object OrientDbSource {

  /**
   * Read `ODocument`s from `className`.
   */
  def create(className: String, settings: OrientDbSourceSettings): Source[OrientDbReadResult[ODocument], NotUsed] =
    Source.fromGraph(
      new OrientDbSourceStage(
        className,
        Option.empty,
        settings))

  /**
   * Read `ODocument`s from `className` or by `query`.
   */
  def create(className: String,
      settings: OrientDbSourceSettings,
      query: String): Source[OrientDbReadResult[ODocument], NotUsed] =
    Source.fromGraph(
      new OrientDbSourceStage(
        className,
        Option(query),
        settings))

  /**
   * Read elements of `T` from `className` or by `query`.
   */
  def typed[T](className: String,
      settings: OrientDbSourceSettings,
      clazz: Class[T],
      query: String = null): Source[OrientDbReadResult[T], NotUsed] =
    Source.fromGraph(
      new OrientDbSourceStage[T](
        className,
        Option(query),
        settings,
        clazz = Some(clazz)))

}
