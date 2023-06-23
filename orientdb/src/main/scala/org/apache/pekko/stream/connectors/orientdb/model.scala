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

package org.apache.pekko.stream.connectors.orientdb

import org.apache.pekko.NotUsed

object OrientDbWriteMessage {
  // Apply method to use when not using passThrough
  def apply[T](oDocument: T): OrientDbWriteMessage[T, NotUsed] =
    OrientDbWriteMessage(oDocument, NotUsed)

  // Java-api - without passThrough
  def create[T](oDocument: T): OrientDbWriteMessage[T, NotUsed] =
    OrientDbWriteMessage(oDocument, NotUsed)

  // Java-api - with passThrough
  def create[T, C](oDocument: T, passThrough: C) =
    OrientDbWriteMessage(oDocument, passThrough)
}

final case class OrientDbWriteMessage[T, C](oDocument: T, passThrough: C)

final case class OrientDbReadResult[T](oDocument: T)
