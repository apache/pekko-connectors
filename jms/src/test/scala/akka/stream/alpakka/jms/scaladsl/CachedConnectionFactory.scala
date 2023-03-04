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

package akka.stream.alpakka.jms.scaladsl

import javax.jms.{ Connection, ConnectionFactory }

import org.apache.activemq.ActiveMQConnection

/**
 * a silly cached connection factory, not thread safe
 */
class CachedConnectionFactory(connFactory: ConnectionFactory) extends ConnectionFactory {

  var cachedConnection: ActiveMQConnection = null

  override def createConnection(): Connection = {
    if (cachedConnection == null) {
      cachedConnection = connFactory.createConnection().asInstanceOf[ActiveMQConnection]
    }
    cachedConnection
  }

  override def createConnection(s: String, s1: String): Connection = cachedConnection

  // added in JMS 2.0
  // see https://github.com/akka/alpakka/issues/1493
  def createContext(x$1: Int): javax.jms.JMSContext = ???
  def createContext(x$1: String, x$2: String, x$3: Int): javax.jms.JMSContext = ???
  def createContext(x$1: String, x$2: String): javax.jms.JMSContext = ???
  def createContext(): javax.jms.JMSContext = ???
}
