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

package akka.stream.alpakka.sns

final class SnsPublishSettings private (val concurrency: Int) {
  require(concurrency > 0)

  def withConcurrency(concurrency: Int): SnsPublishSettings = copy(concurrency = concurrency)

  def copy(concurrency: Int) = new SnsPublishSettings(concurrency)

  override def toString: String =
    "SnsPublishSettings(" +
    s"concurrency=$concurrency" +
    ")"
}

object SnsPublishSettings {
  val Defaults: SnsPublishSettings = new SnsPublishSettings(concurrency = 10)

  /** Scala API */
  def apply(): SnsPublishSettings = Defaults

  /** Java API */
  def create(): SnsPublishSettings = Defaults
}
