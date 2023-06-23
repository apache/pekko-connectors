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
import pekko.http.javadsl.{ model => jm }
import pekko.http.scaladsl.model.headers.{ ModeledCustomHeader, ModeledCustomHeaderCompanion }
import pekko.http.scaladsl.model.{ ContentType, ErrorInfo, IllegalHeaderException }
import pekko.stream.connectors.google.javadsl.XUploadContentType

import scala.util.{ Failure, Success, Try }

/**
 * Models the `X-Upload-Content-Type` header for resumable uploads.
 */
object `X-Upload-Content-Type` extends ModeledCustomHeaderCompanion[`X-Upload-Content-Type`] {
  override def name: String = "X-Upload-Content-Type"
  override def parse(value: String): Try[`X-Upload-Content-Type`] =
    ContentType
      .parse(value)
      .fold(
        errorInfos => Failure(new IllegalHeaderException(errorInfos.headOption.getOrElse(ErrorInfo()))),
        contentType => Success(`X-Upload-Content-Type`(contentType)))
}

final case class `X-Upload-Content-Type` private (contentType: ContentType)
    extends ModeledCustomHeader[`X-Upload-Content-Type`]
    with XUploadContentType {
  override def value(): String = contentType.toString()
  override def renderInRequests(): Boolean = true
  override def renderInResponses(): Boolean = false
  override def companion = `X-Upload-Content-Type`

  /**
   * Java API
   */
  override def getContentType: jm.ContentType = ???
}
