/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
