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

package org.apache.pekko.stream.connectors.mongodb
import org.bson.conversions.Bson

/**
 * @param filter a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
 *               registered
 * @param update a document describing the update, which may not be null. The update to apply must include only update operators. This
 *               can be of any type for which a { @code Codec} is registered
 */
final class DocumentUpdate private (val filter: Bson, val update: Bson) {

  def withFilter(filter: Bson): DocumentUpdate = copy(filter = filter)
  def withUpdate(update: Bson): DocumentUpdate = copy(update = update)

  override def toString: String =
    "DocumentUpdate(" +
    s"filter=$filter," +
    s"update=$update" +
    ")"

  private def copy(filter: Bson = filter, update: Bson = update) =
    new DocumentUpdate(filter, update)
}

object DocumentUpdate {
  def apply(filter: Bson, update: Bson) = new DocumentUpdate(filter, update)

  /**
   * Java Api
   */
  def create(filter: Bson, update: Bson) = DocumentUpdate(filter, update)
}
