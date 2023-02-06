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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.orientdb._
import org.apache.pekko.stream.javadsl.Flow
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.jdk.CollectionConverters._

/**
 * Java API.
 */
object OrientDbFlow {

  /**
   * Flow to write `ODocument`s to OrientDB, elements within one list are stored within one transaction.
   */
  def create(
      className: String,
      settings: OrientDbWriteSettings): Flow[java.util.List[OrientDbWriteMessage[ODocument, NotUsed]],
    java.util.List[OrientDbWriteMessage[ODocument, NotUsed]], NotUsed] =
    org.apache.pekko.stream.scaladsl
      .Flow[java.util.List[OrientDbWriteMessage[ODocument, NotUsed]]]
      .map(_.asScala.toList)
      .via(scaladsl.OrientDbFlow.create(className, settings))
      .map(_.asJava)
      .asJava

  /**
   * Flow to write `ODocument`s to OrientDB, elements within one list are stored within one transaction.
   * Allows a `passThrough` of type `C`.
   */
  def createWithPassThrough[C](
      className: String,
      settings: OrientDbWriteSettings): Flow[java.util.List[OrientDbWriteMessage[ODocument, C]],
    java.util.List[OrientDbWriteMessage[ODocument, C]], NotUsed] =
    org.apache.pekko.stream.scaladsl
      .Flow[java.util.List[OrientDbWriteMessage[ODocument, C]]]
      .map(_.asScala.toList)
      .via(scaladsl.OrientDbFlow.createWithPassThrough[C](className, settings))
      .map(_.asJava)
      .asJava

  /**
   * Flow to write elements of type `T` to OrientDB, elements within one list are stored within one transaction.
   */
  def typed[T](
      className: String,
      settings: OrientDbWriteSettings,
      clazz: Class[T]): Flow[java.util.List[OrientDbWriteMessage[T, NotUsed]], java.util.List[OrientDbWriteMessage[T,
      NotUsed]], NotUsed] =
    org.apache.pekko.stream.scaladsl
      .Flow[java.util.List[OrientDbWriteMessage[T, NotUsed]]]
      .map(_.asScala.toList)
      .via(scaladsl.OrientDbFlow.typed[T](className, settings, clazz))
      .map(_.asJava)
      .asJava

  /**
   * Flow to write elements of type `T` to OrientDB, elements within one list are stored within one transaction.
   * Allows a `passThrough` of type `C`.
   */
  def typedWithPassThrough[T, C](
      className: String,
      settings: OrientDbWriteSettings,
      clazz: Class[T])
      : Flow[java.util.List[OrientDbWriteMessage[T, C]], java.util.List[OrientDbWriteMessage[T, C]], NotUsed] =
    org.apache.pekko.stream.scaladsl
      .Flow[java.util.List[OrientDbWriteMessage[T, C]]]
      .map(_.asScala.toList)
      .via(scaladsl.OrientDbFlow.typedWithPassThrough[T, C](className, settings, clazz))
      .map(_.asJava)
      .asJava
}
