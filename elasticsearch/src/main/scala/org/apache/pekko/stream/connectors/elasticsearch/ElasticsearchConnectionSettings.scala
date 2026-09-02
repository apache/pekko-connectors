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

package org.apache.pekko.stream.connectors.elasticsearch

import org.apache.pekko
import pekko.http.scaladsl.{ ConnectionContext, HttpsConnectionContext }
import pekko.http.scaladsl.model.HttpHeader
import pekko.http.scaladsl.model.HttpHeader.ParsingResult

import javax.net.ssl.SSLContext

import scala.jdk.CollectionConverters._

final class ElasticsearchConnectionSettings private (
    val baseUrl: String,
    val username: Option[String],
    val password: Option[String],
    val headers: List[HttpHeader],
    val connectionContext: Option[HttpsConnectionContext],
    val allowInsecureCredentialsTransport: Boolean) {

  def withBaseUrl(value: String): ElasticsearchConnectionSettings = copy(baseUrl = value)

  def withCredentials(username: String, password: String): ElasticsearchConnectionSettings =
    copy(username = Option(username), password = Option(password))

  def hasCredentialsDefined: Boolean = username.isDefined && password.isDefined

  /** Scala API */
  def withHeaders(headers: List[HttpHeader]): ElasticsearchConnectionSettings =
    copy(headers = headers)

  /** Java API */
  def withHeaders(
      headers: java.util.List[pekko.http.javadsl.model.HttpHeader]): ElasticsearchConnectionSettings = {
    val scalaHeaders = headers.asScala
      .map(x => {
        HttpHeader.parse(x.name(), x.value()) match {
          case ParsingResult.Ok(header, _) => header
          case ParsingResult.Error(error)  =>
            throw new Exception(s"Unable to convert java HttpHeader to scala HttpHeader: ${error.summary}")
        }
      })
      .toList

    copy(headers = scalaHeaders)
  }

  def withSSLContext(
      sslContext: SSLContext): ElasticsearchConnectionSettings = {
    copy(connectionContext = Option(ConnectionContext.httpsClient(sslContext)))
  }

  def hasConnectionContextDefined: Boolean = connectionContext.isDefined

  /**
   * Permit sending credentials over a plain HTTP connection. Defaults to `false`,
   * in which case requests that would send credentials over a non-HTTPS connection
   * fail instead. Enable only for local development and testing, where the traffic
   * cannot be observed.
   *
   * @since 2.0.0
   */
  def withAllowInsecureCredentialsTransport(value: Boolean): ElasticsearchConnectionSettings =
    copy(allowInsecureCredentialsTransport = value)

  private def copy(
      baseUrl: String = baseUrl,
      username: Option[String] = username,
      password: Option[String] = password,
      headers: List[HttpHeader] = headers,
      connectionContext: Option[HttpsConnectionContext] = connectionContext,
      allowInsecureCredentialsTransport: Boolean = allowInsecureCredentialsTransport)
      : ElasticsearchConnectionSettings =
    new ElasticsearchConnectionSettings(baseUrl = baseUrl,
      username = username,
      password = password,
      headers = headers,
      connectionContext = connectionContext,
      allowInsecureCredentialsTransport = allowInsecureCredentialsTransport)

  override def toString = {
    val maskedPassword = password.fold("")(_ => "***")
    val renderedHeaders = headers.mkString(";")
    "ElasticsearchConnectionSettings(" +
    s"baseUrl=$baseUrl," +
    s"username=$username," +
    s"password=$maskedPassword," +
    s"headers=$renderedHeaders," +
    s"connectionContext=$connectionContext," +
    s"allowInsecureCredentialsTransport=$allowInsecureCredentialsTransport)"
  }
}

object ElasticsearchConnectionSettings {

  /** Scala API */
  def apply(baseUrl: String): ElasticsearchConnectionSettings =
    new ElasticsearchConnectionSettings(baseUrl, None, None, List.empty, None, false)

  /** Java API */
  def create(baseUrl: String): ElasticsearchConnectionSettings =
    new ElasticsearchConnectionSettings(baseUrl, None, None, List.empty, None, false)
}
