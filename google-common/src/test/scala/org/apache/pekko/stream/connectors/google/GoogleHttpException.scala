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

package org.apache.pekko.stream.connectors.google

import org.apache.pekko
import pekko.http.scaladsl.model.HttpResponse
import pekko.http.scaladsl.unmarshalling.{ FromResponseUnmarshaller, Unmarshaller }
import pekko.stream.connectors.google.implicits._

final case class GoogleHttpException() extends Exception

object GoogleHttpException {
  implicit val exceptionUnmarshaller: FromResponseUnmarshaller[Throwable] = Unmarshaller.withMaterializer {
    implicit ec => implicit mat => (r: HttpResponse) =>
      r.discardEntityBytes().future().map(_ => GoogleHttpException(): Throwable)
  }.withDefaultRetry
}
