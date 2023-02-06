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

package org.apache.pekko.stream.connectors.reference.testkit
import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.stream.connectors.reference.{ ReferenceReadResult, ReferenceWriteMessage, ReferenceWriteResult }
import org.apache.pekko.util.ByteString

import scala.collection.immutable
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

@ApiMayChange
object MessageFactory {

  @ApiMayChange
  def createReadResult(data: immutable.Seq[ByteString], bytesRead: Try[Int]): ReferenceReadResult =
    new ReferenceReadResult(data, bytesRead)

  /**
   * Java API
   */
  @ApiMayChange
  def createReadResultSuccess(data: java.util.List[ByteString], bytesRead: Int): ReferenceReadResult =
    new ReferenceReadResult(data.asScala.toList, Success(bytesRead))

  /**
   * Java API
   */
  @ApiMayChange
  def createReadResultFailure(data: java.util.List[ByteString], failure: Throwable): ReferenceReadResult =
    new ReferenceReadResult(data.asScala.toList, Failure(failure))

  @ApiMayChange
  def createWriteResult(message: ReferenceWriteMessage, metrics: Map[String, Long], status: Int): ReferenceWriteResult =
    new ReferenceWriteResult(message, metrics, status)

  /**
   * Java API
   */
  @ApiMayChange
  def createWriteResult(message: ReferenceWriteMessage,
      metrics: java.util.Map[String, java.lang.Long],
      status: Int): ReferenceWriteResult =
    new ReferenceWriteResult(message, metrics.asScala.view.mapValues(Long.unbox).toMap, status)

}
