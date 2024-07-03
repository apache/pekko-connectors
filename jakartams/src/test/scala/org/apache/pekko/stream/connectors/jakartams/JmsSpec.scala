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

package org.apache.pekko.stream.connectors.jakartams

import jakarta.jms._
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.testkit.TestKit
import org.mockito.ArgumentMatchers.{ any, anyBoolean, anyInt }
import org.mockito.Mockito.when
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.mockito.MockitoSugar

abstract class JmsSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with Eventually
    with MockitoSugar
    with LogCapturing {

  implicit val system: ActorSystem = ActorSystem(this.getClass.getSimpleName)

  val consumerConfig = system.settings.config.getConfig(JmsConsumerSettings.configPath)
  val producerConfig = system.settings.config.getConfig(JmsProducerSettings.configPath)
  val browseConfig = system.settings.config.getConfig(JmsBrowseSettings.configPath)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  def withConnectionFactory()(test: ConnectionFactory => Unit): Unit =
    withServer() { server =>
      test(server.createConnectionFactory)
    }

  def withServer()(test: EmbeddedActiveMQServer => Unit): Unit = {
    val server = new EmbeddedActiveMQServer(new EmbeddedActiveMQResource)

    try {
      server.start()
      test(server)
      Thread.sleep(500)
    } finally {
      if (server.getServer.getActiveMQServer.isStarted) {
        server.stop()
      }
    }
  }

  def withMockedProducer(test: ProducerMock => Unit): Unit = test(ProducerMock())

  case class ProducerMock(factory: ConnectionFactory = mock[ConnectionFactory],
      connection: Connection = mock[Connection],
      session: Session = mock[Session],
      producer: MessageProducer = mock[MessageProducer],
      queue: jakarta.jms.Queue = mock[jakarta.jms.Queue]) {
    when(factory.createConnection()).thenReturn(connection)
    when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session)
    when(session.createProducer(any[jakarta.jms.Destination])).thenReturn(producer)
    when(session.createQueue(any[String])).thenReturn(queue)
  }

  case class ConsumerMock(factory: ConnectionFactory = mock[ConnectionFactory],
      connection: Connection = mock[Connection],
      session: Session = mock[Session],
      consumer: MessageConsumer = mock[MessageConsumer],
      queue: jakarta.jms.Queue = mock[jakarta.jms.Queue]) {
    when(factory.createConnection()).thenReturn(connection)
    when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session)
    when(session.createConsumer(any[jakarta.jms.Destination])).thenReturn(consumer)
    when(session.createQueue(any[String])).thenReturn(queue)
  }

  def withMockedConsumer(test: ConsumerMock => Unit): Unit = test(ConsumerMock())

}
