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
import pekko.{ Done, NotUsed }
import pekko.stream.connectors.orientdb._
import pekko.stream.scaladsl.{ Keep, Sink }
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.concurrent.Future
import scala.collection.immutable

/**
 * Scala API.
 */
object OrientDbSink {

  /**
   * Sink to write `ODocument`s to OrientDB, elements within one sequence are stored within one transaction.
   */
  def apply(
      className: String,
      settings: OrientDbWriteSettings): Sink[immutable.Seq[OrientDbWriteMessage[ODocument, NotUsed]], Future[Done]] =
    OrientDbFlow.create(className, settings).toMat(Sink.ignore)(Keep.right)

  /**
   * Flow to write elements of type `T` to OrientDB, elements within one sequence are stored within one transaction.
   */
  def typed[T](
      className: String,
      settings: OrientDbWriteSettings,
      clazz: Class[T]): Sink[immutable.Seq[OrientDbWriteMessage[T, NotUsed]], Future[Done]] =
    OrientDbFlow
      .typed(className, settings, clazz)
      .toMat(Sink.ignore)(Keep.right)

}
