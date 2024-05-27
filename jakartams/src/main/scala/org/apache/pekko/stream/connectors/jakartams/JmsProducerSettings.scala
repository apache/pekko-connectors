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

import com.typesafe.config.{ Config, ConfigValueType }
import org.apache.pekko
import org.apache.pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import org.apache.pekko.util.JavaDurationConverters._

import scala.concurrent.duration.FiniteDuration

/**
 * Settings for [[pekko.stream.connectors.jakartams.scaladsl.JmsProducer]] and [[pekko.stream.connectors.jakartams.javadsl.JmsProducer]].
 */
final class JmsProducerSettings private (
    val connectionFactory: jakarta.jms.ConnectionFactory,
    val connectionRetrySettings: ConnectionRetrySettings,
    val sendRetrySettings: SendRetrySettings,
    val destination: Option[Destination],
    val credentials: Option[Credentials],
    val sessionCount: Int,
    val timeToLive: Option[scala.concurrent.duration.Duration],
    val connectionStatusSubscriptionTimeout: scala.concurrent.duration.FiniteDuration)
    extends pekko.stream.connectors.jakartams.JmsSettings {

  /** Factory to use for creating JMS connections. */
  def withConnectionFactory(value: jakarta.jms.ConnectionFactory): JmsProducerSettings = copy(connectionFactory = value)

  /** Configure connection retrying. */
  def withConnectionRetrySettings(value: ConnectionRetrySettings): JmsProducerSettings =
    copy(connectionRetrySettings = value)

  /** Configure re-sending. */
  def withSendRetrySettings(value: SendRetrySettings): JmsProducerSettings = copy(sendRetrySettings = value)

  /** Set a queue name as JMS destination. */
  def withQueue(name: String): JmsProducerSettings = copy(destination = Some(Queue(name)))

  /** Set a topic name as JMS destination. */
  def withTopic(name: String): JmsProducerSettings = copy(destination = Some(Topic(name)))

  /** Set a JMS destination. Allows for custom handling with [[pekko.stream.connectors.jakartams.CustomDestination CustomDestination]]. */
  def withDestination(value: Destination): JmsProducerSettings = copy(destination = Option(value))

  /** Set JMS broker credentials. */
  def withCredentials(value: Credentials): JmsProducerSettings = copy(credentials = Option(value))

  /**
   * Number of parallel sessions to use for sending JMS messages.
   * Increasing the number of parallel sessions increases throughput at the cost of message ordering.
   * While the messages may arrive out of order on the JMS broker, the producer flow outputs messages
   * in the order they are received.
   */
  def withSessionCount(value: Int): JmsProducerSettings = copy(sessionCount = value)

  /**
   * Time messages should be kept on the JMS broker. This setting can be overridden on
   * individual messages. If not set, messages will never expire.
   */
  def withTimeToLive(value: scala.concurrent.duration.Duration): JmsProducerSettings = copy(timeToLive = Option(value))

  /**
   * Java API: Time messages should be kept on the JMS broker. This setting can be overridden on
   * individual messages. If not set, messages will never expire.
   */
  def withTimeToLive(value: java.time.Duration): JmsProducerSettings = copy(timeToLive = Option(value).map(_.asScala))

  /** Timeout for connection status subscriber */
  def withConnectionStatusSubscriptionTimeout(value: FiniteDuration): JmsProducerSettings =
    copy(connectionStatusSubscriptionTimeout = value)

  /** Java API: Timeout for connection status subscriber */
  def withConnectionStatusSubscriptionTimeout(value: java.time.Duration): JmsProducerSettings =
    copy(connectionStatusSubscriptionTimeout = value.asScala)

  private def copy(
      connectionFactory: jakarta.jms.ConnectionFactory = connectionFactory,
      connectionRetrySettings: ConnectionRetrySettings = connectionRetrySettings,
      sendRetrySettings: SendRetrySettings = sendRetrySettings,
      destination: Option[Destination] = destination,
      credentials: Option[Credentials] = credentials,
      sessionCount: Int = sessionCount,
      timeToLive: Option[scala.concurrent.duration.Duration] = timeToLive,
      connectionStatusSubscriptionTimeout: scala.concurrent.duration.FiniteDuration =
        connectionStatusSubscriptionTimeout): JmsProducerSettings = new JmsProducerSettings(
    connectionFactory = connectionFactory,
    connectionRetrySettings = connectionRetrySettings,
    sendRetrySettings = sendRetrySettings,
    destination = destination,
    credentials = credentials,
    sessionCount = sessionCount,
    timeToLive = timeToLive,
    connectionStatusSubscriptionTimeout = connectionStatusSubscriptionTimeout)

  override def toString =
    "JmsProducerSettings(" +
    s"connectionFactory=$connectionFactory," +
    s"connectionRetrySettings=$connectionRetrySettings," +
    s"sendRetrySettings=$sendRetrySettings," +
    s"destination=$destination," +
    s"credentials=$credentials," +
    s"sessionCount=$sessionCount," +
    s"timeToLive=${timeToLive.map(_.toCoarsest)}," +
    s"connectionStatusSubscriptionTimeout=${connectionStatusSubscriptionTimeout.toCoarsest}" +
    ")"
}

object JmsProducerSettings {

  val configPath = "pekko.connectors.jakartams.producer"

  /**
   * Reads from the given config.
   *
   * @param c Config instance read configuration from
   * @param connectionFactory Factory to use for creating JMS connections.
   */
  def apply(c: Config, connectionFactory: jakarta.jms.ConnectionFactory): JmsProducerSettings = {
    def getOption[A](path: String, read: Config => A): Option[A] =
      if (c.hasPath(path) && (c.getValue(path).valueType() != ConfigValueType.STRING || c.getString(path) != "off"))
        Some(read(c))
      else None

    val connectionRetrySettings = ConnectionRetrySettings(c.getConfig("connection-retry"))
    val sendRetrySettings = SendRetrySettings(c.getConfig("send-retry"))
    val credentials = getOption("credentials", c => Credentials(c.getConfig("credentials")))
    val sessionCount = c.getInt("session-count")
    val timeToLive = getOption("time-to-live", _.getDuration("time-to-live").asScala)
    val connectionStatusSubscriptionTimeout = c.getDuration("connection-status-subscription-timeout").asScala
    new JmsProducerSettings(
      connectionFactory,
      connectionRetrySettings,
      sendRetrySettings,
      destination = None,
      credentials,
      sessionCount,
      timeToLive,
      connectionStatusSubscriptionTimeout)
  }

  /**
   * Reads from the default config provided by the actor system at `pekko.connectors.jakartams.producer`.
   *
   * @param actorSystem The actor system
   * @param connectionFactory Factory to use for creating JMS connections.
   */
  def apply(actorSystem: ActorSystem, connectionFactory: jakarta.jms.ConnectionFactory): JmsProducerSettings =
    apply(actorSystem.settings.config.getConfig(configPath), connectionFactory)

  /**
   * Reads from the default config provided by the actor system at `pekko.connectors.jakartams.producer`.
   *
   * @param actorSystem The actor system
   * @param connectionFactory Factory to use for creating JMS connections.
   */
  def apply(actorSystem: ClassicActorSystemProvider,
      connectionFactory: jakarta.jms.ConnectionFactory): JmsProducerSettings =
    apply(actorSystem.classicSystem, connectionFactory)

  /**
   * Java API: Reads from the given config.
   *
   * @param c Config instance read configuration from
   * @param connectionFactory Factory to use for creating JMS connections.
   */
  def create(c: Config, connectionFactory: jakarta.jms.ConnectionFactory): JmsProducerSettings =
    apply(c, connectionFactory)

  /**
   * Java API: Reads from the default config provided by the actor system at `pekko.connectors.jakartams.producer`.
   *
   * @param actorSystem The actor system
   * @param connectionFactory Factory to use for creating JMS connections.
   */
  def create(actorSystem: ActorSystem, connectionFactory: jakarta.jms.ConnectionFactory): JmsProducerSettings =
    apply(actorSystem, connectionFactory)

  /**
   * Java API: Reads from the default config provided by the actor system at `pekko.connectors.jakartams.producer`.
   *
   * @param actorSystem The actor system
   * @param connectionFactory Factory to use for creating JMS connections.
   */
  def create(actorSystem: ClassicActorSystemProvider,
      connectionFactory: jakarta.jms.ConnectionFactory): JmsProducerSettings =
    apply(actorSystem.classicSystem, connectionFactory)

}
