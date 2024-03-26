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

package org.apache.pekko.stream.connectors.jms

import javax.jms._
import jmstestkit.JmsBroker

import scala.util.Random

/**
 * Creates a single server and connection factory which is shared for all tests.
 */
abstract class JmsSharedServerSpec extends JmsSpec {
  private var jmsBroker: JmsBroker = _
  private var connectionFactory: ConnectionFactory = _

  override def beforeAll(): Unit = {
    jmsBroker = JmsBroker()
    connectionFactory = jmsBroker.createConnectionFactory
    Thread.sleep(500)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    if (jmsBroker != null && jmsBroker.isStarted)
      jmsBroker.stop()
  }

  protected def isQueueEmpty(queueName: String): Boolean = jmsBroker.service.checkQueueSize(queueName)

  override def withConnectionFactory()(test: ConnectionFactory => Unit): Unit =
    test(connectionFactory)

  def createName(prefix: String) = prefix + Random.nextInt().toString

}
