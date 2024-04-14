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

package org.apache.pekko.stream.connectors.google

import org.apache.pekko
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.InternalApi
import pekko.http.javadsl.{ model => jm }
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.model.headers.BasicHttpCredentials
import pekko.http.scaladsl.settings.ConnectionPoolSettings
import pekko.http.scaladsl.{ Http, HttpsConnectionContext }
import pekko.http.{ javadsl => jh }
import pekko.stream.connectors.google.auth.Credentials
import pekko.stream.connectors.google.http.{ ForwardProxyHttpsContext, ForwardProxyPoolSettings }
import pekko.stream.connectors.google.implicits._
import pekko.util.JavaDurationConverters._
import pekko.util.OptionConverters._
import com.typesafe.config.Config

import java.time
import java.util.Optional
import scala.concurrent.duration._

object GoogleSettings {
  val ConfigPath: String = "pekko.connectors.google"

  /**
   * Reads from the given config.
   */
  def apply(c: Config)(implicit system: ClassicActorSystemProvider): GoogleSettings = {

    val credentials = Credentials(c.getConfig("credentials"))
    val requestSettings = RequestSettings(c)

    GoogleSettings(credentials.projectId, credentials, requestSettings)
  }

  /**
   * Java API: Reads from the given config.
   */
  def create(c: Config, system: ClassicActorSystemProvider): GoogleSettings =
    apply(c)(system)

  /**
   * Reads from the config at the given path.
   */
  def apply(path: String)(implicit system: ClassicActorSystemProvider): GoogleSettings =
    GoogleExt(system).settings(path)

  /**
   * Java API: Reads from the config at the given path.
   */
  def create(path: String, system: ClassicActorSystemProvider): GoogleSettings =
    apply(path)(system)

  /**
   * Scala API: Creates [[GoogleSettings]] from the [[com.typesafe.config.Config Config]] attached to an actor system.
   */
  def apply()(implicit system: ClassicActorSystemProvider, dummy: DummyImplicit): GoogleSettings = apply(system)

  /**
   * Scala API: Creates [[GoogleSettings]] from the [[com.typesafe.config.Config Config]] attached to an [[pekko.actor.ActorSystem]].
   */
  def apply(system: ClassicActorSystemProvider): GoogleSettings = GoogleExt(system.classicSystem).settings

  implicit def settings(implicit system: ClassicActorSystemProvider): GoogleSettings = apply(system)

  /**
   * Java API: Creates [[GoogleSettings]] from the [[com.typesafe.config.Config Config]] attached to an actor system.
   */
  def create(system: ClassicActorSystemProvider): GoogleSettings = apply(system)

  /**
   * Java API
   */
  def create(projectId: String, credentials: Credentials, requestSettings: RequestSettings): GoogleSettings =
    GoogleSettings(projectId, credentials, requestSettings)

}

@InternalApi
final case class GoogleSettings(projectId: String,
    credentials: Credentials,
    requestSettings: RequestSettings) {
  def getProjectId: String = projectId
  def getCredentials: Credentials = credentials
  def getRequestSettings: RequestSettings = requestSettings

  def withProjectId(projectId: String): GoogleSettings =
    copy(projectId = projectId)
  def withCredentials(credentials: Credentials): GoogleSettings =
    copy(credentials = credentials)
  def withRequestSettings(requestSettings: RequestSettings): GoogleSettings =
    copy(requestSettings = requestSettings)
}

object RequestSettings {

  def apply(c: Config)(implicit system: ClassicActorSystemProvider): RequestSettings = {
    val retrySettings = RetrySettings(c.getConfig("retry-settings"))
    val maybeForwardProxy =
      if (c.hasPath("forward-proxy") && c.hasPath("forward-proxy.host") && c.hasPath("forward-proxy.port"))
        Some(ForwardProxy(c.getConfig("forward-proxy")))
      else
        None
    RequestSettings(
      Some(c.getString("user-ip")).filterNot(_.isEmpty),
      Some(c.getString("quota-user")).filterNot(_.isEmpty),
      c.getBoolean("pretty-print"),
      java.lang.Math.toIntExact(c.getBytes("upload-chunk-size")),
      retrySettings,
      maybeForwardProxy)
  }

  def create(config: Config)(implicit system: ClassicActorSystemProvider): RequestSettings = apply(config)

  def create(userIp: Optional[String],
      quotaUser: Optional[String],
      prettyPrint: Boolean,
      chunkSize: Int,
      retrySettings: RetrySettings,
      forwardProxy: Optional[ForwardProxy]): RequestSettings =
    apply(userIp.toScala, quotaUser.toScala, prettyPrint, chunkSize, retrySettings, forwardProxy.toScala)
}

@InternalApi
final case class RequestSettings(
    userIp: Option[String],
    quotaUser: Option[String],
    prettyPrint: Boolean,
    uploadChunkSize: Int,
    retrySettings: RetrySettings,
    forwardProxy: Option[ForwardProxy]) {

  require(
    (uploadChunkSize >= (256 * 1024)) & (uploadChunkSize % (256 * 1024) == 0),
    "Chunk size must be a multiple of 256 KiB")

  def getUserIp: Optional[String] = userIp.toJava
  def getQuotaUser: Optional[String] = quotaUser.toJava
  def getPrettyPrint: Boolean = prettyPrint
  def getUploadChunkSize: Int = uploadChunkSize
  def getRetrySettings: RetrySettings = retrySettings
  def getForwardProxy: Option[ForwardProxy] = forwardProxy

  def withUserIp(userIp: Option[String]): RequestSettings =
    copy(userIp = userIp)
  def withUserIp(userIp: Optional[String]): RequestSettings =
    copy(userIp = userIp.toScala)
  def withQuotaUser(quotaUser: Option[String]): RequestSettings =
    copy(quotaUser = quotaUser)
  def withQuotaUser(quotaUser: Optional[String]): RequestSettings =
    copy(quotaUser = quotaUser.toScala)
  def withPrettyPrint(prettyPrint: Boolean): RequestSettings =
    copy(prettyPrint = prettyPrint)
  def withUploadChunkSize(uploadChunkSize: Int): RequestSettings =
    copy(uploadChunkSize = uploadChunkSize)
  def withRetrySettings(retrySettings: RetrySettings): RequestSettings =
    copy(retrySettings = retrySettings)
  def withForwardProxy(forwardProxy: Option[ForwardProxy]): RequestSettings =
    copy(forwardProxy = forwardProxy)
  def withForwardProxy(forwardProxy: Optional[ForwardProxy]): RequestSettings =
    copy(forwardProxy = forwardProxy.toScala)

  // Cache query string
  private[google] def query =
    ("prettyPrint" -> prettyPrint.toString) +: ("userIp" -> userIp) ?+: ("quotaUser" -> quotaUser) ?+: Query.Empty
  private[google] val queryString = query.toString
  private[google] val `&queryString` = "&".concat(queryString)
}

object RetrySettings {

  def apply(config: Config): RetrySettings =
    RetrySettings(
      config.getInt("max-retries"),
      config.getDuration("min-backoff").asScala,
      config.getDuration("max-backoff").asScala,
      config.getDouble("random-factor"))

  def create(config: Config): RetrySettings = apply(config)

  def create(
      maxRetries: Int, minBackoff: time.Duration, maxBackoff: time.Duration, randomFactor: Double): RetrySettings =
    apply(
      maxRetries,
      minBackoff.asScala,
      maxBackoff.asScala,
      randomFactor)
}

final case class RetrySettings @InternalApi private (maxRetries: Int,
    minBackoff: FiniteDuration,
    maxBackoff: FiniteDuration,
    randomFactor: Double) {
  def getMaxRetries: Int = maxRetries
  def getMinBackoff: time.Duration = minBackoff.asJava
  def getMaxBackoff: time.Duration = maxBackoff.asJava
  def getRandomFactor: Double = randomFactor

  def withMaxRetries(maxRetries: Int): RetrySettings =
    copy(maxRetries = maxRetries)
  def withMinBackoff(minBackoff: FiniteDuration): RetrySettings =
    copy(minBackoff = minBackoff)
  def withMinBackoff(minBackoff: time.Duration): RetrySettings =
    copy(minBackoff = minBackoff.asScala)
  def withMaxBackoff(maxBackoff: FiniteDuration): RetrySettings =
    copy(maxBackoff = maxBackoff)
  def withMaxBackoff(maxBackoff: time.Duration): RetrySettings =
    copy(maxBackoff = maxBackoff.asScala)
  def withRandomFactor(randomFactor: Double): RetrySettings =
    copy(randomFactor = randomFactor)
}

object ForwardProxy {

  def apply(c: Config)(implicit system: ClassicActorSystemProvider): ForwardProxy = {
    val scheme =
      if (c.hasPath("scheme")) c.getString("scheme")
      else "https"

    val maybeCredentials =
      if (c.hasPath("credentials"))
        Some(BasicHttpCredentials(c.getString("credentials.username"), c.getString("credentials.password")))
      else None

    val maybeTrustPem =
      if (c.hasPath("trust-pem"))
        Some(c.getString("trust-pem"))
      else
        None

    ForwardProxy(scheme, c.getString("host"), c.getInt("port"), maybeCredentials, maybeTrustPem)
  }

  def create(c: Config, system: ClassicActorSystemProvider): ForwardProxy =
    apply(c)(system)

  def apply(scheme: String,
      host: String,
      port: Int,
      credentials: Option[BasicHttpCredentials],
      trustPem: Option[String])(implicit system: ClassicActorSystemProvider): ForwardProxy =
    ForwardProxy(
      trustPem.fold(Http(system.classicSystem).defaultClientHttpsContext)(ForwardProxyHttpsContext(_)),
      ForwardProxyPoolSettings(scheme, host, port, credentials)(system.classicSystem))

  def create(scheme: String,
      host: String,
      port: Int,
      credentials: Optional[jm.headers.BasicHttpCredentials],
      trustPem: Optional[String],
      system: ClassicActorSystemProvider): ForwardProxy =
    apply(scheme, host, port, credentials.toScala.map(_.asInstanceOf[BasicHttpCredentials]), trustPem.toScala)(system)

  def create(
      connectionContext: jh.HttpConnectionContext, poolSettings: jh.settings.ConnectionPoolSettings): ForwardProxy =
    apply(connectionContext.asInstanceOf[HttpsConnectionContext], poolSettings.asInstanceOf[ConnectionPoolSettings])
}

final case class ForwardProxy @InternalApi private (connectionContext: HttpsConnectionContext,
    poolSettings: ConnectionPoolSettings) {
  def getConnectionContext: jh.HttpsConnectionContext = connectionContext
  def getPoolSettings: jh.settings.ConnectionPoolSettings = poolSettings
  def withConnectionContext(connectionContext: HttpsConnectionContext): ForwardProxy =
    copy(connectionContext = connectionContext)
  def withConnectionContext(connectionContext: jh.HttpsConnectionContext): ForwardProxy =
    copy(connectionContext = connectionContext.asInstanceOf[HttpsConnectionContext])
  def withPoolSettings(poolSettings: ConnectionPoolSettings): ForwardProxy =
    copy(poolSettings = poolSettings)
  def withPoolSettings(poolSettings: jh.settings.ConnectionPoolSettings): ForwardProxy =
    copy(poolSettings = poolSettings.asInstanceOf[ConnectionPoolSettings])
}
