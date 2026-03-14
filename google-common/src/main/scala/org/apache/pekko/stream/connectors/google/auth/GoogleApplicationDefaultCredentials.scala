/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.google.RequestSettings
import com.google.auth.oauth2.{ GoogleCredentials => OAuthGoogleCredentials }
import com.google.auth.{ Credentials => GoogleCredentials }

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

/**
 * Credentials that delegate to Google's `google-auth-library-java` for credential resolution.
 * This uses `GoogleCredentials.getApplicationDefault()` which supports all GCP environments
 * including GKE Workload Identity.
 */
@InternalApi
private[connectors] object GoogleApplicationDefaultCredentials {

  def apply(projectId: String, scopes: Set[String]): GoogleApplicationDefaultCredentials = {
    val googleCredentials = OAuthGoogleCredentials.getApplicationDefault
      .createScoped(scopes.asJava)
    new GoogleApplicationDefaultCredentials(projectId, googleCredentials)
  }

}

/**
 * Credentials backed by Google's `google-auth-library-java`.
 */
@InternalApi
private[connectors] final class GoogleApplicationDefaultCredentials(
    override val projectId: String,
    private val googleCredentials: OAuthGoogleCredentials) extends Credentials {

  override def asGoogle(implicit ec: ExecutionContext, settings: RequestSettings): GoogleCredentials =
    googleCredentials
}
