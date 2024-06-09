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

package org.apache.pekko.stream.connectors.jakartams.scaladsl

import jakarta.jms.{ Connection, ConnectionFactory }

/**
 * a silly cached connection factory, not thread safe
 */
class CachedConnectionFactory(connFactory: ConnectionFactory) extends ConnectionFactory {

  var cachedConnection: Connection = null

  override def createConnection(): Connection = {
    if (cachedConnection == null) {
      cachedConnection = connFactory.createConnection()
    }
    cachedConnection
  }

  override def createConnection(s: String, s1: String): Connection = cachedConnection

  def createContext(x$1: Int): jakarta.jms.JMSContext = ???
  def createContext(x$1: String, x$2: String, x$3: Int): jakarta.jms.JMSContext = ???
  def createContext(x$1: String, x$2: String): jakarta.jms.JMSContext = ???
  def createContext(): jakarta.jms.JMSContext = ???
}
