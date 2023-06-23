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

package org.apache.pekko.stream.connectors.orientdb.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.orientdb._
import pekko.stream.connectors.orientdb.impl.OrientDbFlowStage
import pekko.stream.scaladsl.Flow
import com.orientechnologies.orient.core.record.impl.ODocument
import scala.collection.immutable

/**
 * Scala API.
 */
object OrientDbFlow {

  /**
   * Flow to write `ODocument`s to OrientDB, elements within one list are stored within one transaction.
   */
  def create(
      className: String,
      settings: OrientDbWriteSettings): Flow[immutable.Seq[OrientDbWriteMessage[ODocument, NotUsed]],
    immutable.Seq[OrientDbWriteMessage[ODocument, NotUsed]], NotUsed] =
    Flow
      .fromGraph(
        new OrientDbFlowStage[ODocument, NotUsed](
          className,
          settings,
          None))

  /**
   * Flow to write `ODocument`s to OrientDB, elements within one sequence are stored within one transaction.
   * Allows a `passThrough` of type `C`.
   */
  def createWithPassThrough[C](
      className: String,
      settings: OrientDbWriteSettings): Flow[immutable.Seq[OrientDbWriteMessage[ODocument, C]],
    immutable.Seq[OrientDbWriteMessage[ODocument, C]], NotUsed] =
    Flow
      .fromGraph(
        new OrientDbFlowStage[ODocument, C](
          className,
          settings,
          None))

  /**
   * Flow to write elements of type `T` to OrientDB, elements within one sequence are stored within one transaction.
   */
  def typed[T](
      className: String,
      settings: OrientDbWriteSettings,
      clazz: Class[T]): Flow[immutable.Seq[OrientDbWriteMessage[T, NotUsed]], immutable.Seq[OrientDbWriteMessage[T,
      NotUsed]], NotUsed] =
    Flow
      .fromGraph(
        new OrientDbFlowStage[T, NotUsed](
          className,
          settings,
          Some(clazz)))

  /**
   * Flow to write elements of type `T` to OrientDB, elements within one sequence are stored within one transaction.
   * Allows a `passThrough` of type `C`.
   */
  def typedWithPassThrough[T, C](
      className: String,
      settings: OrientDbWriteSettings,
      clazz: Class[T])
      : Flow[immutable.Seq[OrientDbWriteMessage[T, C]], immutable.Seq[OrientDbWriteMessage[T, C]], NotUsed] =
    Flow
      .fromGraph(
        new OrientDbFlowStage[T, C](
          className,
          settings,
          Some(clazz)))
}
