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

package org.apache.pekko.stream.connectors.elasticsearch.testkit

import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.elasticsearch.{ ReadResult, WriteMessage, WriteResult }
import pekko.util.OptionConverters._

object MessageFactory {

  /**
   * Scala API
   * For use with testing.
   */
  @ApiMayChange
  def createReadResult[T](
      id: String,
      source: T,
      version: Option[Long]): ReadResult[T] = new ReadResult(
    id,
    source,
    version)

  /**
   * Java API
   * For use with testing.
   */
  @ApiMayChange
  def createReadResult[T](
      id: String,
      source: T,
      version: java.util.Optional[Long]): ReadResult[T] = new ReadResult(
    id,
    source,
    version.toScala)
  @ApiMayChange
  def createWriteResult[T, PT](
      message: WriteMessage[T, PT],
      error: Option[String]): WriteResult[T, PT] = new WriteResult(
    message,
    error)

  /**
   * Java API
   */
  @ApiMayChange
  def createWriteResult[T, PT](
      message: WriteMessage[T, PT],
      error: java.util.Optional[String]): WriteResult[T, PT] = new WriteResult(
    message,
    error.toScala)

}
