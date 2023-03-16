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

package org.apache.pekko.stream.connectors.googlecloud.storage

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

final class FailedUpload private (
    val reasons: Seq[Throwable]) extends Exception(reasons.map(_.getMessage).mkString(", ")) {

  /** Java API */
  def getReasons: java.util.List[Throwable] = reasons.asJava
}

object FailedUpload {

  def apply(reasons: Seq[Throwable]) = new FailedUpload(reasons)

  /** Java API */
  def create(reasons: java.util.List[Throwable]) = FailedUpload(reasons.asScala.toList)
}
