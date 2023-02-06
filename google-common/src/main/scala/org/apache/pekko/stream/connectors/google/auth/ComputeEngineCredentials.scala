/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.google.RequestSettings

import java.time.Clock
import scala.concurrent.Future

@InternalApi
private[auth] object ComputeEngineCredentials {

  def apply()(implicit system: ClassicActorSystemProvider): Future[Credentials] =
    GoogleComputeMetadata
      .getProjectId()
      .map(new ComputeEngineCredentials(_))(system.classicSystem.dispatcher)

}

@InternalApi
private final class ComputeEngineCredentials(projectId: String)(implicit mat: Materializer)
    extends OAuth2Credentials(projectId) {
  override protected def getAccessToken()(implicit mat: Materializer,
      settings: RequestSettings,
      clock: Clock): Future[AccessToken] =
    GoogleComputeMetadata.getAccessToken()
}
