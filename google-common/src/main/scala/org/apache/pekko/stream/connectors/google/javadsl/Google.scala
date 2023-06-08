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

package org.apache.pekko.stream.connectors.google.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ClassicActorSystemProvider
import pekko.http.javadsl.model.{ HttpRequest, HttpResponse }
import pekko.http.javadsl.unmarshalling.Unmarshaller
import pekko.http.scaladsl.{ model => sm, unmarshalling }
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.google.scaladsl.{ Google => ScalaGoogle }
import pekko.stream.javadsl.{ Sink, Source }
import pekko.util.ByteString
import pekko.util.FutureConverters._

import java.util.concurrent.CompletionStage
import scala.language.implicitConversions

/**
 * Java API: Provides methods to interface with Google APIs
 */
object Google extends Google

private[connectors] trait Google {

  /**
   * Makes a request and returns the unmarshalled response. Authentication is handled automatically.
   * Retries the request if the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]].
   *
   * @param request the [[pekko.http.javadsl.model.HttpRequest]] to make
   * @tparam T the data model for the resource
   * @return a [[java.util.concurrent.CompletionStage]] containing the unmarshalled response
   */
  final def singleRequest[T](request: HttpRequest,
      unmarshaller: Unmarshaller[HttpResponse, T],
      settings: GoogleSettings,
      system: ClassicActorSystemProvider): CompletionStage[T] =
    ScalaGoogle.singleRequest[T](request)(unmarshaller.asScala, system, settings).asJava

  /**
   * Makes a series of requests to page through a resource. Authentication is handled automatically.
   * Requests are retried if the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]].
   *
   * @param request the initial [[pekko.http.javadsl.model.HttpRequest]] to make; must be a `GET` request
   * @tparam Out the data model for each page of the resource
   * @return a [[pekko.stream.javadsl.Source]] that emits an `Out` for each page of the resource
   */
  final def paginatedRequest[Out <: Paginated](request: HttpRequest,
      unmarshaller: Unmarshaller[HttpResponse, Out]): Source[Out, NotUsed] = {
    implicit val um: unmarshalling.Unmarshaller[HttpResponse, Out] = unmarshaller.asScala
    ScalaGoogle.paginatedRequest[Out](request).asJava
  }

  /**
   * Makes a series of requests to upload a stream of bytes to a media endpoint. Authentication is handled automatically.
   * If the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]] the upload will attempt to recover and continue.
   *
   * @param request the [[pekko.http.javadsl.model.HttpRequest]] to initiate the upload; must be a `POST` request with query `uploadType=resumable`
   *                and optionally a [[pekko.stream.connectors.google.javadsl.XUploadContentType]] header
   * @tparam Out the data model for the resource
   * @return a [[pekko.stream.javadsl.Sink]] that materializes a [[java.util.concurrent.CompletionStage]] containing the unmarshalled resource
   */
  final def resumableUpload[Out](
      request: HttpRequest,
      unmarshaller: Unmarshaller[HttpResponse, Out]): Sink[ByteString, CompletionStage[Out]] =
    ScalaGoogle.resumableUpload(request)(unmarshaller.asScala).mapMaterializedValue(_.asJava).asJava

  private implicit def requestAsScala(request: HttpRequest): sm.HttpRequest = request.asInstanceOf[sm.HttpRequest]
}
