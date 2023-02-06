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

package org.apache.pekko.stream.connectors.slick

import slick.jdbc.{ JdbcBackend, JdbcProfile }

package object scaladsl {

  /**
   * Scala API: Represents an "open" Slick database and its database (type) profile.
   *
   * <b>NOTE</b>: these databases need to be closed after creation to
   * avoid leaking database resources like active connection pools, etc.
   */
  type SlickSession = javadsl.SlickSession

  /**
   * Scala API: Methods for "opening" Slick databases for use.
   *
   * <b>NOTE</b>: databases created through these methods will need to be
   * closed after creation to avoid leaking database resources like active
   * connection pools, etc.
   */
  object SlickSession extends javadsl.SlickSessionFactory {
    def forDbAndProfile(db: JdbcBackend#Database, profile: JdbcProfile): SlickSession =
      new SlickSessionDbAndProfileBackedImpl(db, profile)
  }
}
