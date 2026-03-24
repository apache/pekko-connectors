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
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.google.RequestSettings
import com.google.auth.{ Credentials => GoogleCredentials }

import scala.concurrent.ExecutionContext

/**
 * Wraps a user-provided [[com.google.auth.Credentials]] instance for use with Google connectors.
 * This allows users to bring their own credentials resolved through any mechanism.
 *
 * @param projectId the GCP project id
 * @param credentials the user-provided Google credentials
 * @since 2.0.0
 */
@ApiMayChange
final class ProvidedCredentials(
    override val projectId: String,
    private val credentials: GoogleCredentials) extends Credentials {

  override def asGoogle(implicit ec: ExecutionContext, settings: RequestSettings): GoogleCredentials =
    credentials

  /**
   * Java API
   */
  def getProjectId: String = projectId
}

object ProvidedCredentials {

  /**
   * Scala API: Create credentials from a user-provided [[com.google.auth.Credentials]] instance.
   */
  def apply(projectId: String, credentials: GoogleCredentials): ProvidedCredentials =
    new ProvidedCredentials(projectId, credentials)

  /**
   * Java API: Create credentials from a user-provided [[com.google.auth.Credentials]] instance.
   */
  def create(projectId: String, credentials: GoogleCredentials): ProvidedCredentials =
    apply(projectId, credentials)
}
