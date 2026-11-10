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

package org.apache.pekko.stream.connectors.amqp.scaladsl

import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.{ ConcurrentLinkedQueue, CountDownLatch, ExecutorService, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger

import org.apache.pekko.stream.connectors.amqp._
import com.rabbitmq.client.{ AddressResolver, Connection, ConnectionFactory, RecoverableConnection }
import org.mockito.Mockito.{ mock, verify, when }
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Try

class AmqpConnectionProvidersSpec extends AmqpSpec with Eventually {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private def join(threads: Thread*): Unit = threads.foreach { thread =>
    thread.join(patienceConfig.timeout.toMillis)
    thread.isAlive should be(false)
  }

  "The AMQP Default Connection Providers" should {
    "create a new connection per invocation of LocalAmqpConnection" in {
      val connectionProvider = AmqpLocalConnectionProvider
      val connection1 = connectionProvider.get
      val connection2 = connectionProvider.get
      (connection1 should not).equal(connection2)
      connectionProvider.release(connection1)
      connectionProvider.release(connection2)
    }

    "not error if releasing an already closed LocalAmqpConnection" in {
      val connectionProvider = AmqpLocalConnectionProvider
      val connection1 = connectionProvider.get
      connectionProvider.release(connection1)
      connectionProvider.release(connection1)
    }

    "create a new connection per invocation of AmqpConnectionUri" in {
      val connectionProvider = AmqpUriConnectionProvider("amqp://localhost:5672")
      val connection1 = connectionProvider.get
      val connection2 = connectionProvider.get
      (connection1 should not).equal(connection2)
      connectionProvider.release(connection1)
      connectionProvider.release(connection2)
    }

    "not error if releasing an already closed AmqpConnectionUri" in {
      val connectionProvider = AmqpUriConnectionProvider("amqp://localhost:5672")
      val connection1 = connectionProvider.get
      connectionProvider.release(connection1)
      connectionProvider.release(connection1)
    }

    "create a new connection per invocation of AmqpConnectionDetails" in {
      val connectionProvider = AmqpDetailsConnectionProvider("localhost", 5672)
      val connection1 = connectionProvider.get
      val connection2 = connectionProvider.get
      (connection1 should not).equal(connection2)
      connectionProvider.release(connection1)
      connectionProvider.release(connection2)
    }

    "not error if releasing an already closed AmqpConnectionDetails" in {
      val connectionProvider = AmqpDetailsConnectionProvider("localhost", 5672)
      val connection1 = connectionProvider.get
      connectionProvider.release(connection1)
      connectionProvider.release(connection1)
    }

    "create a new connection per invocation of AmqpConnectionFactory" in {
      val connectionFactory = new ConnectionFactory()
      val connectionProvider =
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672)
      val connection1 = connectionProvider.get
      val connection2 = connectionProvider.get
      (connection1 should not).equal(connection2)
      connectionProvider.release(connection1)
      connectionProvider.release(connection2)
    }

    "not error if releasing an already closed AmqpConnectionFactory" in {
      val connectionFactory = new ConnectionFactory()
      val connectionProvider =
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672)
      val connection1 = connectionProvider.get
      connectionProvider.release(connection1)
      connectionProvider.release(connection1)
    }
  }

  "The AMQP Reusable Connection Provider with automatic release" should {
    "reuse the same connection from LocalAmqpConnection and release it when the last client disconnects" in {
      val connectionProvider = AmqpLocalConnectionProvider
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(true)
      connection2.isOpen should be(true)
      reusableConnectionProvider.release(connection2)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "reuse the same connection from AmqpConnectionUri and release it when the last client disconnects" in {
      val connectionProvider = AmqpUriConnectionProvider("amqp://localhost:5672")
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(true)
      connection2.isOpen should be(true)
      reusableConnectionProvider.release(connection2)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "reuse the same connection from AmqpConnectionDetails and release it when the last client disconnects" in {
      val connectionProvider = AmqpDetailsConnectionProvider("localhost", 5672)
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(true)
      connection2.isOpen should be(true)
      reusableConnectionProvider.release(connection2)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "reuse the same connection from AmqpConnectionFactory and release it when the last client disconnects" in {
      val connectionFactory = new ConnectionFactory()
      val connectionProvider =
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672)
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(true)
      connection2.isOpen should be(true)
      reusableConnectionProvider.release(connection2)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "replace a closed connection" in {
      val reusableConnectionProvider = AmqpCachedConnectionProvider(AmqpDetailsConnectionProvider("localhost", 5672))
      val closedConnection = reusableConnectionProvider.get
      closedConnection.close()
      val connection = reusableConnectionProvider.get
      (connection should not).equal(closedConnection)
      reusableConnectionProvider.release(closedConnection)
      connection.isOpen should be(true)
      reusableConnectionProvider.release(connection)
      connection.isOpen should be(false)
    }

    "reuse a recoverable connection while it is closed" in {
      val connection = mock(classOf[RecoverableConnection])
      var connectionsCreated = 0
      val connectionFactory = new ConnectionFactory() {
        override def newConnection(es: ExecutorService, ar: AddressResolver, name: String): Connection = {
          connectionsCreated += 1
          connection
        }
      }
      val reusableConnectionProvider = AmqpCachedConnectionProvider(
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672))

      when(connection.isOpen).thenReturn(true)
      (reusableConnectionProvider.get should be).theSameInstanceAs(connection)
      when(connection.isOpen).thenReturn(false)
      (reusableConnectionProvider.get should be).theSameInstanceAs(connection)
      when(connection.isOpen).thenReturn(true)
      (reusableConnectionProvider.get should be).theSameInstanceAs(connection)
      connectionsCreated should be(1)

      reusableConnectionProvider.release(connection)
      reusableConnectionProvider.release(connection)
      reusableConnectionProvider.release(connection)
      verify(connection).close()
    }

    "abort a recoverable connection released while it is recovering" in {
      val connection = mock(classOf[RecoverableConnection])
      val connectionFactory = new ConnectionFactory() {
        override def newConnection(es: ExecutorService, ar: AddressResolver, name: String): Connection = connection
      }
      val reusableConnectionProvider = AmqpCachedConnectionProvider(
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672))

      when(connection.isOpen).thenReturn(false)
      reusableConnectionProvider.get
      reusableConnectionProvider.release(connection)
      verify(connection).abort()
    }

    "block concurrent clients while the connection is being opened" in {
      val connecting = new CountDownLatch(1)
      val connect = new CountDownLatch(1)
      val attempts = new AtomicInteger()
      val connectionFactory = new ConnectionFactory() {
        override def newConnection(es: ExecutorService, ar: AddressResolver, name: String): Connection = {
          attempts.incrementAndGet()
          connecting.countDown()
          connect.await(patienceConfig.timeout.toMillis, TimeUnit.MILLISECONDS) should be(true)
          super.newConnection(es, ar, name)
        }
      }
      val reusableConnectionProvider = AmqpCachedConnectionProvider(
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672))
      val results = new ConcurrentLinkedQueue[Try[Connection]]()
      val first = new Thread(() => results.add(Try(reusableConnectionProvider.get)): Unit)
      val second = new Thread(() => results.add(Try(reusableConnectionProvider.get)): Unit)
      try {
        first.start()
        connecting.await(patienceConfig.timeout.toMillis, TimeUnit.MILLISECONDS) should be(true)
        second.start()
        eventually(second.getState should be(Thread.State.WAITING))
        attempts.get should be(1)
      } finally connect.countDown()
      join(first, second)
      val connections = results.asScala.toList.map(_.get)
      attempts.get should be(1)
      connections should have size 2
      connections.toSet should have size 1
      connections.foreach(reusableConnectionProvider.release)
      connections.head.isOpen should be(false)
    }

    "not block releases while a connection is being opened" in {
      val connecting = new CountDownLatch(1)
      val connect = new CountDownLatch(1)
      val attempts = new AtomicInteger()
      val connectionFactory = new ConnectionFactory() {
        override def newConnection(es: ExecutorService, ar: AddressResolver, name: String): Connection = {
          if (attempts.incrementAndGet() > 1) {
            connecting.countDown()
            connect.await(patienceConfig.timeout.toMillis, TimeUnit.MILLISECONDS) should be(true)
          }
          super.newConnection(es, ar, name)
        }
      }
      connectionFactory.setAutomaticRecoveryEnabled(false)
      val reusableConnectionProvider = AmqpCachedConnectionProvider(
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672))
      val closedConnection = reusableConnectionProvider.get
      closedConnection.close()
      val results = new ConcurrentLinkedQueue[Try[Connection]]()
      val releases = new ConcurrentLinkedQueue[Try[Unit]]()
      val client = new Thread(() => results.add(Try(reusableConnectionProvider.get)): Unit)
      val releasing = new Thread(() => releases.add(Try(reusableConnectionProvider.release(closedConnection))): Unit)
      try {
        client.start()
        connecting.await(patienceConfig.timeout.toMillis, TimeUnit.MILLISECONDS) should be(true)
        releasing.start()
        eventually(releasing.getState should be(Thread.State.TERMINATED))
      } finally connect.countDown()
      join(client)
      releases.asScala.foreach(_.get)
      val connection = results.asScala.head.get
      reusableConnectionProvider.release(connection)
      connection.isOpen should be(false)
    }

    "fail concurrent clients together if opening the connection fails" in {
      val connecting = new CountDownLatch(1)
      val fail = new CountDownLatch(1)
      val attempts = new AtomicInteger()
      val connectionFactory = new ConnectionFactory() {
        override def newConnection(es: ExecutorService, ar: AddressResolver, name: String): Connection = {
          attempts.incrementAndGet()
          connecting.countDown()
          fail.await(patienceConfig.timeout.toMillis, TimeUnit.MILLISECONDS) should be(true)
          throw new ConnectException("Connection refused")
        }
      }
      val reusableConnectionProvider = AmqpCachedConnectionProvider(
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672))
      val failures = new ConcurrentLinkedQueue[Throwable]()
      val first = new Thread(() => Try(reusableConnectionProvider.get).failed.foreach(failures.add))
      val second = new Thread(() => Try(reusableConnectionProvider.get).failed.foreach(failures.add))
      try {
        first.start()
        connecting.await(patienceConfig.timeout.toMillis, TimeUnit.MILLISECONDS) should be(true)
        second.start()
        eventually(second.getState should be(Thread.State.WAITING))
      } finally fail.countDown()
      join(first, second)
      attempts.get should be(1)
      val (originals, wrapped) = failures.asScala.toList.partition(_.isInstanceOf[ConnectException])
      originals should have size 1
      wrapped should have size 1
      wrapped.head shouldBe an[IOException]
      (wrapped.head.getCause should be).theSameInstanceAs(originals.head)
    }
  }

  "The AMQP Reusable Connection Provider without automatic release" should {
    "reuse the same connection from LocalAmqpConnection" in {
      val connectionProvider = AmqpLocalConnectionProvider
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider).withAutomaticRelease(false)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "reuse the same connection from AmqpConnectionUri" in {
      val connectionProvider = AmqpUriConnectionProvider("amqp://localhost:5672")
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider).withAutomaticRelease(false)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "reuse the same connection from AmqpConnectionDetails" in {
      val connectionProvider = AmqpDetailsConnectionProvider("localhost", 5672)
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider).withAutomaticRelease(false)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "reuse the same connection from AmqpConnectionFactory" in {
      val connectionFactory = new ConnectionFactory()
      val connectionProvider =
        AmqpConnectionFactoryConnectionProvider(connectionFactory).withHostAndPort("localhost", 5672)
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider).withAutomaticRelease(false)
      val connection1 = reusableConnectionProvider.get
      val connection2 = reusableConnectionProvider.get
      connection1 should equal(connection2)
      reusableConnectionProvider.release(connection1)
      connection1.isOpen should be(false)
      connection2.isOpen should be(false)
    }

    "release all tracked connections using a replaced connection" in {
      val reusableConnectionProvider =
        AmqpCachedConnectionProvider(AmqpDetailsConnectionProvider("localhost", 5672)).withAutomaticRelease(false)
      val original = reusableConnectionProvider.get
      original.close()
      val replacement = reusableConnectionProvider.get
      try {
        (replacement should not).equal(original)
        reusableConnectionProvider.release(original)
        replacement.isOpen should be(false)
        intercept[IllegalStateException](reusableConnectionProvider.release(original))
      } finally if (replacement.isOpen) replacement.close()
    }

    "not leave the provider in an invalid state if getting the connection fails" in {
      val connectionProvider = AmqpDetailsConnectionProvider("localhost", 5673)
      val reusableConnectionProvider = AmqpCachedConnectionProvider(connectionProvider).withAutomaticRelease(false)
      try reusableConnectionProvider.get
      catch { case e: Throwable => e shouldBe a[ConnectException] }
      try reusableConnectionProvider.get
      catch { case e: Throwable => e shouldBe a[ConnectException] }
    }
  }
}
