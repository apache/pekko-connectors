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

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.http.scaladsl.model.headers.OAuth2BearerToken
import pekko.stream.connectors.google.RequestSettings
import com.google.auth.{ Credentials => GoogleCredentials }
import com.typesafe.config.Config

import java.net.URI
import java.util
import scala.concurrent.{ ExecutionContext, Future }

@InternalApi
private[connectors] object NoCredentials {

  def apply(c: Config): NoCredentials = NoCredentials(c.getString("project-id"), c.getString("token"))

}

@InternalApi
private[auth] final case class NoCredentials private (projectId: String, token: String) extends Credentials {

  private val futureToken = Future.successful(OAuth2BearerToken(token))

  override def get()(implicit ec: ExecutionContext, settings: RequestSettings): Future[OAuth2BearerToken] =
    futureToken

  override def asGoogle(implicit ec: ExecutionContext, settings: RequestSettings): GoogleCredentials =
    new GoogleCredentials {
      override def getAuthenticationType: String = "<none>"
      override def getRequestMetadata(uri: URI): util.Map[String, util.List[String]] = util.Collections.emptyMap()
      override def hasRequestMetadata: Boolean = false
      override def hasRequestMetadataOnly: Boolean = true
      override def refresh(): Unit = ()
    }
}
