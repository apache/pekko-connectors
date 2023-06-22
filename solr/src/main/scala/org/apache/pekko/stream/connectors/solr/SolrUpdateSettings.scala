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

package org.apache.pekko.stream.connectors.solr

final class SolrUpdateSettings private (
    val commitWithin: Int) {

  /**
   * Set max time (in ms) before a commit will happen
   */
  def withCommitWithin(value: Int): SolrUpdateSettings = copy(commitWithin = value)

  private def copy(
      commitWithin: Int): SolrUpdateSettings = new SolrUpdateSettings(
    commitWithin = commitWithin)

  override def toString =
    "SolrUpdateSettings(" +
    s"commitWithin=$commitWithin" +
    ")"
}

object SolrUpdateSettings {

  val Defaults = new SolrUpdateSettings(-1)

  /** Scala API */
  def apply(): SolrUpdateSettings = Defaults

  /** Java API */
  def create(): SolrUpdateSettings = Defaults

}
