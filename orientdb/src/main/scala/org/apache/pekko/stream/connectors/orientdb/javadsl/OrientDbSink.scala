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

package org.apache.pekko.stream.connectors.orientdb.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.stream.connectors.orientdb._
import pekko.stream.javadsl._
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Java API.
 */
object OrientDbSink {

  /**
   * Sink to write `ODocument`s to OrientDB, elements within one list are stored within one transaction.
   */
  def create(
      className: String,
      settings: OrientDbWriteSettings)
      : Sink[java.util.List[OrientDbWriteMessage[ODocument, NotUsed]], CompletionStage[Done]] =
    OrientDbFlow
      .create(className, settings)
      .toMat(Sink.ignore[java.util.List[OrientDbWriteMessage[ODocument, NotUsed]]](),
        Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Flow to write elements of type `T` to OrientDB, elements within one list are stored within one transaction.
   */
  def typed[T](className: String,
      settings: OrientDbWriteSettings,
      clazz: Class[T]): Sink[java.util.List[OrientDbWriteMessage[T, NotUsed]], CompletionStage[Done]] =
    OrientDbFlow
      .typed[T](className, settings, clazz)
      .toMat(Sink.ignore[java.util.List[OrientDbWriteMessage[T, NotUsed]]](),
        Keep.right[NotUsed, CompletionStage[Done]])
}
