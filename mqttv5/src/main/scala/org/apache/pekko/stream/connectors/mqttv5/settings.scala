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

package org.apache.pekko.stream.connectors.mqttv5

import java.nio.charset.StandardCharsets
import java.util.Properties

import scala.collection.immutable
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import org.apache.pekko
import pekko.japi.Pair
import pekko.util.JavaDurationConverters._
import org.eclipse.paho.mqttv5.client.MqttClientPersistence
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.eclipse.paho.mqttv5.common.{ MqttMessage => PahoMqttMessage }

/**
 * Quality of Service constants as defined in
 * [[https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901234]]
 */
sealed abstract class MqttQoS {
  def value: Int
}

/**
 * Quality of Service constants as defined in
 * [[https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901234]]
 */
object MqttQoS {

  /**
   * Quality of Service 0 - indicates that a message should be delivered at most once (zero or one times). The message
   * will not be persisted to disk, and will not be acknowledged across the network. This QoS is the fastest, but should
   * only be used for messages which are not valuable.
   */
  object AtMostOnce extends MqttQoS {
    val value: Int = 0
  }

  /**
   * Quality of Service 1 - indicates that a message should be delivered at least once (one or more times). The message
   * can only be delivered safely if it can be persisted, so the application must supply a means of persistence using
   * [[MqttConnectionSettings]]. If a persistence mechanism is not specified, the message will not be delivered in the
   * event of a client failure. The message will be acknowledged across the network.
   */
  object AtLeastOnce extends MqttQoS {
    val value: Int = 1
  }

  /**
   * Quality of Service 2 - indicates that a message should be delivered once. The message will be persisted to disk,
   * and will be subject to a two-phase acknowledgement across the network. The message can only be delivered safely
   * if it can be persisted, so the application must supply a means of persistence using [[MqttConnectionSettings]].
   * If a persistence mechanism is not specified, the message will not be delivered in the event of a client failure.
   */
  object ExactlyOnce extends MqttQoS {
    val value: Int = 2
  }

  /**
   * Java API
   *
   * Quality of Service 0 - indicates that a message should be delivered at most once (zero or one times). The message
   * will not be persisted to disk, and will not be acknowledged across the network. This QoS is the fastest, but should
   * only be used for messages which are not valuable.
   */
  def atMostOnce: MqttQoS = AtMostOnce

  /**
   * Java API
   *
   * Quality of Service 1 - indicates that a message should be delivered at least once (one or more times). The message
   * can only be delivered safely if it can be persisted, so the application must supply a means of persistence using
   * [[MqttConnectionSettings]]. If a persistence mechanism is not specified, the message will not be delivered in the
   * event of a client failure. The message will be acknowledged across the network.
   */
  def atLeastOnce: MqttQoS = AtLeastOnce

  /**
   * Java API
   *
   * Quality of Service 2 - indicates that a message should be delivered once. The message will be persisted to disk,
   * and will be subject to a two-phase acknowledgement across the network. The message can only be delivered safely
   * if it can be persisted, so the application must supply a means of persistence using [[MqttConnectionSettings]].
   * If a persistence mechanism is not specified, the message will not be delivered in the event of a client failure.
   */
  def exactlyOnce: MqttQoS = ExactlyOnce
}

/**
 * The mapping of topics to subscribe to and the requested Quality of Service ([[MqttQoS]]) per topic.
 */
final class MqttSubscriptions private (
    val subscriptions: Map[String, MqttQoS]) {

  /** Scala API */
  def withSubscriptions(subscriptions: Map[String, MqttQoS]): MqttSubscriptions =
    new MqttSubscriptions(subscriptions)

  /** Java API */
  def withSubscriptions(subscriptions: java.util.List[pekko.japi.Pair[String, MqttQoS]]): MqttSubscriptions =
    new MqttSubscriptions(subscriptions.asScala.map(_.toScala).toMap)

  /** Add this subscription to the map of subscriptions configured already. */
  def addSubscription(topic: String, qos: MqttQoS): MqttSubscriptions =
    new MqttSubscriptions(this.subscriptions.updated(topic, qos))
}

/**
 * The mapping of topics to subscribe to and the requested Quality of Service ([[MqttQoS]]) per topic.
 */
object MqttSubscriptions {
  val empty = new MqttSubscriptions(Map.empty)

  /** Scala API */
  def apply(subscriptions: Map[String, MqttQoS]): MqttSubscriptions =
    new MqttSubscriptions(subscriptions)

  /** Scala API */
  def apply(topic: String, qos: MqttQoS): MqttSubscriptions =
    new MqttSubscriptions(Map(topic -> qos))

  /** Scala API */
  def apply(subscription: (String, MqttQoS)): MqttSubscriptions =
    new MqttSubscriptions(Map(subscription))

  /** Java API */
  def create(subscriptions: java.util.List[pekko.japi.Pair[String, MqttQoS]]): MqttSubscriptions =
    new MqttSubscriptions(subscriptions.asScala.map(_.toScala).toMap)

  /** Java API */
  def create(topic: String, qos: MqttQoS): MqttSubscriptions =
    new MqttSubscriptions(Map(topic -> qos))

}

private[mqttv5] final case class CleanStartSettings(
    enabled: Boolean,
    sessionExpiration: Option[FiniteDuration]
)

private[mqttv5] final case class DisconnectSettings(
    quiesceTimeout: FiniteDuration,
    timeout: FiniteDuration,
    sendDisconnectPacket: Boolean
)

private[mqttv5] sealed trait AuthSettings {
  def asString: String
}

private[mqttv5] object AuthSettings {
  case object Disabled extends AuthSettings {
    override lazy val asString: String = "Disabled"
  }

  final case class Simple(
      username: String,
      password: String
  ) extends AuthSettings {
    override lazy val asString: String = s"Simple(username=$username)"
  }

  final case class Enhanced(
      method: String,
      initialData: Array[Byte],
      authPacketHandler: (Int, MqttProperties) => (Int, MqttProperties)
  ) extends AuthSettings {
    override lazy val asString: String = s"Enhanced(method=$method)"
  }
}

private[mqttv5] final case class MqttOfflinePersistenceSettings(
    bufferSize: Int = 5000,
    deleteOldestMessage: Boolean = false,
    persistBuffer: Boolean = true
)

/**
 * Connection settings passed to the underlying Paho client.
 *
 * Java docs for `MqttConnectionOptions` are not available;
 * see [[https://github.com/eclipse-paho/paho.mqtt.java/issues/1012]] or
 * [[org.eclipse.paho.mqttv5.client.MqttConnectionOptions]] for more info
 */
final class MqttConnectionSettings private (
    val broker: String,
    val clientId: String,
    val persistence: MqttClientPersistence,
    val cleanStart: CleanStartSettings,
    val disconnect: DisconnectSettings,
    val auth: AuthSettings,
    val offlinePersistence: Option[MqttOfflinePersistenceSettings],
    val automaticReconnect: Boolean,
    val keepAliveInterval: FiniteDuration,
    val connectionTimeout: FiniteDuration,
    val serverUris: Array[String],
    val will: Option[MqttMessage],
    val sslProperties: Map[String, String],
    val socketFactory: Option[javax.net.ssl.SSLSocketFactory],
    val sslHostnameVerifier: Option[javax.net.ssl.HostnameVerifier]
) {
  def asMqttConnectionOptions(): MqttConnectionOptions = {
    val options = new MqttConnectionOptions

    auth match {
      case AuthSettings.Disabled =>
      // do nothing

      case AuthSettings.Simple(username, password) =>
        options.setUserName(username)
        options.setPassword(password.getBytes(StandardCharsets.UTF_8))

      case AuthSettings.Enhanced(method, initialData, _) =>
        options.setAuthMethod(method)
        options.setAuthData(initialData)
    }

    options.setCleanStart(cleanStart.enabled)
    if (!cleanStart.enabled) {
      cleanStart.sessionExpiration match {
        case Some(value) =>
          options.setSessionExpiryInterval(value.toSeconds)

        case None =>
          // The documentation of `setSessionExpiryInterval` is not correct in this case -
          // the server will treat a `null` as `0` and will immediately expire the session.
          //
          // Instead, we set the expiration to the maximum allowed value (~136 years of retention;
          // see `org.eclipse.paho.mqttv5.common.packet.MqttProperties#setSessionExpiryInterval`)
          options.setSessionExpiryInterval(Int.MaxValue.toLong * 2)
      }
    }

    if (serverUris.nonEmpty) {
      options.setServerURIs(serverUris)
    }

    will.foreach { w =>
      options.setWill(
        w.topic,
        new PahoMqttMessage(
          w.payload.toArray,
          w.qos.getOrElse(MqttQoS.atLeastOnce).value,
          w.retained,
          /* properties */ null
        )
      )
    }

    if (sslProperties.nonEmpty) {
      val properties = new Properties()
      sslProperties.foreach { case (key, value) => properties.setProperty(key, value) }
      options.setSSLProperties(properties)
    }

    options.setAutomaticReconnect(automaticReconnect)
    options.setKeepAliveInterval(keepAliveInterval.toSeconds.toInt)
    options.setConnectionTimeout(connectionTimeout.toSeconds.toInt)
    socketFactory.foreach(options.setSocketFactory)
    sslHostnameVerifier.foreach(options.setSSLHostnameVerifier)

    options
  }

  def withBroker(value: String): MqttConnectionSettings =
    copy(broker = value)

  def withClientId(value: String): MqttConnectionSettings =
    copy(clientId = value)

  def withPersistence(value: MqttClientPersistence): MqttConnectionSettings =
    copy(persistence = value)

  def withAuth(
      username: String,
      password: String
  ): MqttConnectionSettings =
    copy(auth = AuthSettings.Simple(username = username, password = password))

  /** Scala API */
  def withAuth(
      method: String,
      initialData: Array[Byte],
      authPacketHandler: (Int, MqttProperties) => (Int, MqttProperties)
  ): MqttConnectionSettings =
    copy(
      auth = AuthSettings.Enhanced(
        method = method,
        initialData = initialData,
        authPacketHandler = authPacketHandler
      )
    )

  /** Java API */
  def withAuth(
      method: String,
      initialData: Array[Byte],
      authPacketHandler: java.util.function.BiFunction[Int, MqttProperties, Pair[Int, MqttProperties]]
  ): MqttConnectionSettings =
    withAuth(
      method = method,
      initialData = initialData,
      authPacketHandler = (reasonCode: Int, properties: MqttProperties) =>
        authPacketHandler.apply(reasonCode, properties).toScala
    )

  def withSocketFactory(value: javax.net.ssl.SSLSocketFactory): MqttConnectionSettings =
    copy(socketFactory = Option(value))

  def withCleanStart(enabled: Boolean): MqttConnectionSettings =
    copy(cleanStart = CleanStartSettings(enabled = enabled, sessionExpiration = None))

  /** Scala API */
  def withCleanStart(enabled: Boolean, sessionExpiration: Option[FiniteDuration]): MqttConnectionSettings =
    copy(cleanStart = CleanStartSettings(enabled, sessionExpiration))

  /** Java API */
  def withCleanStart(enabled: Boolean, sessionExpiration: java.time.Duration): MqttConnectionSettings =
    copy(cleanStart = CleanStartSettings(enabled, Option(sessionExpiration).map(_.asScala)))

  def withWill(value: MqttMessage): MqttConnectionSettings =
    copy(will = Option(value))

  def withAutomaticReconnect(value: Boolean): MqttConnectionSettings =
    copy(automaticReconnect = value)

  /** Scala API */
  def withKeepAliveInterval(value: FiniteDuration): MqttConnectionSettings =
    copy(keepAliveInterval = value)

  /** Java API */
  def withKeepAliveInterval(value: java.time.Duration): MqttConnectionSettings =
    withKeepAliveInterval(value.asScala)

  /** Scala API */
  def withConnectionTimeout(value: FiniteDuration): MqttConnectionSettings =
    copy(connectionTimeout = value)

  /** Java API */
  def withConnectionTimeout(value: java.time.Duration): MqttConnectionSettings =
    withConnectionTimeout(value.asScala)

  /** Scala API */
  def withDisconnectQuiesceTimeout(value: FiniteDuration): MqttConnectionSettings =
    copy(disconnect = disconnect.copy(quiesceTimeout = value))

  /** Java API */
  def withDisconnectQuiesceTimeout(value: java.time.Duration): MqttConnectionSettings =
    withDisconnectQuiesceTimeout(value.asScala)

  /** Scala API */
  def withDisconnectTimeout(value: FiniteDuration): MqttConnectionSettings =
    copy(disconnect = disconnect.copy(timeout = value))

  /** Java API */
  def withDisconnectTimeout(value: java.time.Duration): MqttConnectionSettings =
    withDisconnectTimeout(value.asScala)

  def withServerUri(value: String): MqttConnectionSettings =
    copy(serverUris = Array(value))

  /** Scala API */
  def withServerUris(value: immutable.Seq[String]): MqttConnectionSettings =
    copy(serverUris = value.toArray)

  /** Java API */
  def withServerUris(value: java.util.List[String]): MqttConnectionSettings =
    copy(serverUris = value.asScala.toArray)

  def withSslHostnameVerifier(value: javax.net.ssl.HostnameVerifier): MqttConnectionSettings =
    copy(sslHostnameVerifier = Option(value))

  /** Scala API */
  def withSslProperties(value: Map[String, String]): MqttConnectionSettings =
    copy(sslProperties = value)

  /** Java API */
  def withSslProperties(value: java.util.Map[String, String]): MqttConnectionSettings =
    withSslProperties(value = value.asScala.toMap)

  def withOfflinePersistenceSettings(
      bufferSize: Int = 5000,
      deleteOldestMessage: Boolean = false,
      persistBuffer: Boolean = true
  ): MqttConnectionSettings =
    copy(
      offlinePersistence = Option(
        MqttOfflinePersistenceSettings(
          bufferSize = bufferSize,
          deleteOldestMessage = deleteOldestMessage,
          persistBuffer = persistBuffer
        )
      )
    )

  private def copy(
      broker: String = broker,
      clientId: String = clientId,
      persistence: MqttClientPersistence = persistence,
      cleanStart: CleanStartSettings = cleanStart,
      disconnect: DisconnectSettings = disconnect,
      offlinePersistence: Option[MqttOfflinePersistenceSettings] = offlinePersistence,
      auth: AuthSettings = auth,
      automaticReconnect: Boolean = automaticReconnect,
      keepAliveInterval: FiniteDuration = keepAliveInterval,
      connectionTimeout: FiniteDuration = connectionTimeout,
      serverUris: Array[String] = serverUris,
      will: Option[MqttMessage] = will,
      sslProperties: Map[String, String] = sslProperties,
      socketFactory: Option[javax.net.ssl.SSLSocketFactory] = socketFactory,
      sslHostnameVerifier: Option[javax.net.ssl.HostnameVerifier] = sslHostnameVerifier
  ): MqttConnectionSettings =
    new MqttConnectionSettings(
      broker = broker,
      clientId = clientId,
      persistence = persistence,
      disconnect = disconnect,
      offlinePersistence = offlinePersistence,
      auth = auth,
      cleanStart = cleanStart,
      automaticReconnect = automaticReconnect,
      keepAliveInterval = keepAliveInterval,
      connectionTimeout = connectionTimeout,
      serverUris = serverUris,
      will = will,
      sslProperties = sslProperties,
      socketFactory = socketFactory,
      sslHostnameVerifier = sslHostnameVerifier
    )

  override def toString: String =
    "MqttConnectionSettings(" +
    s"broker=$broker," +
    s"clientId=$clientId," +
    s"persistence=$persistence," +
    s"disconnect=$disconnect," +
    s"offlinePersistence=$offlinePersistence," +
    s"auth=${auth.asString}," +
    s"cleanStart=$cleanStart," +
    s"automaticReconnect=$automaticReconnect," +
    s"keepAliveInterval=$keepAliveInterval," +
    s"connectionTimeout=$connectionTimeout," +
    s"serverUris=$serverUris," +
    s"will=$will," +
    s"sslProperties=$sslProperties," +
    s"socketFactory=$socketFactory," +
    s"sslHostnameVerifier=$sslHostnameVerifier" +
    ")"
}

/**
 * Factory for connection settings passed to the underlying Paho client.
 *
 * Java docs for `MqttConnectionOptions` are not available;
 * see [[https://github.com/eclipse-paho/paho.mqtt.java/issues/1012]] or
 * [[org.eclipse.paho.mqttv5.client.MqttConnectionOptions]] for more info
 */
object MqttConnectionSettings {

  /** Scala API */
  def apply(
      broker: String,
      clientId: String,
      persistence: MqttClientPersistence
  ): MqttConnectionSettings = {
    val defaults = new MqttConnectionOptions

    new MqttConnectionSettings(
      broker = broker,
      clientId = clientId,
      persistence = persistence,
      disconnect = DisconnectSettings(
        quiesceTimeout = 30.seconds,
        timeout = 10.seconds,
        sendDisconnectPacket = true
      ),
      offlinePersistence = None,
      auth = AuthSettings.Disabled,
      cleanStart = CleanStartSettings(enabled = defaults.isCleanStart, sessionExpiration = None),
      automaticReconnect = false,
      keepAliveInterval = defaults.getKeepAliveInterval.seconds,
      connectionTimeout = defaults.getConnectionTimeout.seconds,
      serverUris = Array.empty,
      will = None,
      sslProperties = Map.empty,
      socketFactory = None,
      sslHostnameVerifier = None
    )
  }

  /** Java API */
  def create(
      broker: String,
      clientId: String,
      persistence: MqttClientPersistence
  ): MqttConnectionSettings = apply(
    broker = broker,
    clientId = clientId,
    persistence = persistence
  )
}
