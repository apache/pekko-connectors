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

package org.apache.pekko.stream.connectors.google.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ClassicActorSystemProvider
import pekko.http.scaladsl.model.HttpRequest
import pekko.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import pekko.stream.connectors.google.http.GoogleHttp
import pekko.stream.connectors.google.{ GoogleSettings, PaginatedRequest, ResumableUpload }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.util.ByteString

import scala.concurrent.Future

/**
 * Provides methods to interface with Google APIs
 */
object Google extends Google

private[connectors] trait Google {

  /**
   * Makes a request and returns the unmarshalled response. Authentication is handled automatically.
   * Retries the request if the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]].
   *
   * @param request the [[pekko.http.scaladsl.model.HttpRequest]] to make
   * @tparam T the data model for the resource
   * @return a [[scala.concurrent.Future]] containing the unmarshalled response
   */
  final def singleRequest[T: FromResponseUnmarshaller](
      request: HttpRequest)(implicit system: ClassicActorSystemProvider, settings: GoogleSettings): Future[T] =
    GoogleHttp().singleAuthenticatedRequest[T](request)

  /**
   * Makes a series of requests to page through a resource. Authentication is handled automatically.
   * Requests are retried if the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]].
   *
   * @param request the initial [[pekko.http.scaladsl.model.HttpRequest]] to make; must be a `GET` request
   * @tparam Out the data model for each page of the resource
   * @return a [[pekko.stream.scaladsl.Source]] that emits an `Out` for each page of the resource
   */
  final def paginatedRequest[Out: Paginated: FromResponseUnmarshaller](request: HttpRequest): Source[Out, NotUsed] =
    PaginatedRequest[Out](request)

  /**
   * Makes a series of requests to upload a stream of bytes to a media endpoint. Authentication is handled automatically.
   * If the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]] the upload will attempt to recover and continue.
   *
   * @param request the [[pekko.http.scaladsl.model.HttpRequest]] to initiate the upload; must be a `POST` request with query `uploadType=resumable`
   *                and optionally a [[pekko.stream.connectors.google.scaladsl.`X-Upload-Content-Type`]] header
   * @tparam Out the data model for the resource
   * @return a [[pekko.stream.scaladsl.Sink]] that materializes a [[scala.concurrent.Future]] containing the unmarshalled resource
   */
  final def resumableUpload[Out: FromResponseUnmarshaller](request: HttpRequest): Sink[ByteString, Future[Out]] =
    ResumableUpload[Out](request)

}
