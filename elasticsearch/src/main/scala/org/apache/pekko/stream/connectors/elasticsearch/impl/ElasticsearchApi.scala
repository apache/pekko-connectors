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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.event.Logging
import pekko.http.scaladsl.HttpExt
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.headers.BasicHttpCredentials
import pekko.stream.connectors.elasticsearch.ElasticsearchConnectionSettings

import scala.concurrent.Future

@InternalApi private[impl] object ElasticsearchApi {

  private val logSource = "ElasticsearchApi"

  private def insecureCredentialsMessage(scheme: String): String =
    ("Credentials are configured but the request URI scheme is '%s' (not 'https'). " +
    "Sending BasicAuth credentials over plain HTTP is insecure. " +
    "Configure a HTTPS base URL to use with credentials, or set " +
    "withAllowInsecureCredentialsTransport(true) to allow this.").format(scheme)

  /**
   * Decide whether credentials may be sent for this request.
   *
   * Returns `Left` with the error message when the request must be rejected,
   * or `Right` with an optional warning message when it may proceed.
   */
  private[impl] def checkCredentialsTransport(
      scheme: String,
      connectionSettings: ElasticsearchConnectionSettings): Either[String, Option[String]] =
    if (scheme.toLowerCase == "https") Right(None)
    else if (connectionSettings.allowInsecureCredentialsTransport) Right(Some(insecureCredentialsMessage(scheme)))
    else Left(insecureCredentialsMessage(scheme))

  def executeRequest(
      request: HttpRequest,
      connectionSettings: ElasticsearchConnectionSettings)(implicit http: HttpExt): Future[HttpResponse] = {
    val connectionContext = connectionSettings.connectionContext.getOrElse(http.defaultClientHttpsContext)
    if (connectionSettings.hasCredentialsDefined) {
      checkCredentialsTransport(request.uri.scheme, connectionSettings) match {
        case Left(msg) =>
          Logging(http.system, logSource).error(msg)
          // a failed Future rather than a thrown exception, so that the calling
          // stages can route this through their own failure handling
          Future.failed(new IllegalStateException(msg))
        case Right(warning) =>
          warning.foreach(Logging(http.system, logSource).warning(_))
          http.singleRequest(
            request.addCredentials(
              BasicHttpCredentials(connectionSettings.username.get, connectionSettings.password.get)),
            connectionContext = connectionContext)
      }
    } else {
      http.singleRequest(request, connectionContext = connectionContext)
    }
  }
}
