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

package org.apache.pekko.stream.connectors.google.javadsl

import org.apache.pekko
import pekko.stream.connectors.google.scaladsl
import pekko.util.OptionConverters._

import java.util

/**
 * Models a paginated resource
 */
trait Paginated {

  /**
   * Returns the token for the next page, if present
   */
  def getPageToken: util.Optional[String]
}

private[connectors] object Paginated {
  implicit object paginatedIsPaginated extends scaladsl.Paginated[Paginated] {
    override def pageToken(paginated: Paginated): Option[String] = paginated.getPageToken.toScala
  }
}
