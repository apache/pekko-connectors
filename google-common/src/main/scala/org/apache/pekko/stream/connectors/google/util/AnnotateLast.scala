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

package org.apache.pekko.stream.connectors.google.util

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.scaladsl.Flow

@InternalApi
private[google] object MaybeLast {
  def unapply[T](maybeLast: MaybeLast[T]): Some[T] = Some(maybeLast.value)
}

@InternalApi
private[google] sealed abstract class MaybeLast[+T] {
  def value: T
  def isLast: Boolean
  def map[S](f: T => S): MaybeLast[S]
}

@InternalApi
private[google] final case class NotLast[+T](value: T) extends MaybeLast[T] {
  override def isLast: Boolean = false
  override def map[S](f: T => S): NotLast[S] = NotLast(f(value))
}

@InternalApi
private[google] final case class Last[+T](value: T) extends MaybeLast[T] {
  override def isLast: Boolean = true
  override def map[S](f: T => S): Last[S] = Last(f(value))
}

@InternalApi
private[google] object AnnotateLast {

  def apply[T]: Flow[T, MaybeLast[T], NotUsed] =
    Flow[T]
      .statefulMap(() => Option.empty[T])((maybePreviousElement, elem) => {
          maybePreviousElement match {
            case Some(previousElem) => (Some(elem), Some(NotLast(previousElem)))
            case None               => (Some(elem), None)
          }
        }, _.map(elem => Some(Last(elem))))
      .collect {
        case Some(elem) => elem
      }
}
