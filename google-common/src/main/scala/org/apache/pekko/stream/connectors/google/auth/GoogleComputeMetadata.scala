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
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pekko.http.scaladsl.model.HttpMethods.GET
import pekko.http.scaladsl.model.HttpRequest
import pekko.http.scaladsl.model.headers.RawHeader
import pekko.http.scaladsl.unmarshalling.Unmarshal
import pekko.stream.Materializer

import java.time.Clock
import scala.concurrent.Future

@InternalApi
private[auth] object GoogleComputeMetadata {

  private val metadataUrl = "http://metadata.google.internal/computeMetadata/v1"
  private val tokenUrl = s"$metadataUrl/instance/service-accounts/default/token"
  private val projectIdUrl = s"$metadataUrl/project/project-id"
  private val `Metadata-Flavor` = RawHeader("Metadata-Flavor", "Google")

  private val tokenRequest = HttpRequest(GET, tokenUrl).addHeader(`Metadata-Flavor`)
  private val projectIdRequest = HttpRequest(GET, projectIdUrl).addHeader(`Metadata-Flavor`)

  def getAccessToken()(
      implicit mat: Materializer,
      clock: Clock): Future[AccessToken] = {
    import SprayJsonSupport._
    import mat.executionContext
    implicit val system = mat.system
    for {
      response <- Http().singleRequest(tokenRequest)
      token <- Unmarshal(response.entity).to[AccessToken]
    } yield token
  }

  def getProjectId()(
      implicit mat: Materializer): Future[String] = {
    import mat.executionContext
    implicit val system = mat.system
    for {
      response <- Http().singleRequest(projectIdRequest)
      projectId <- Unmarshal(response.entity).to[String]
    } yield projectId
  }
}
