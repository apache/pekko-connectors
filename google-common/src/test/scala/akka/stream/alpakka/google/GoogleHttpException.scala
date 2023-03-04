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

package akka.stream.alpakka.google

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.{ FromResponseUnmarshaller, Unmarshaller }
import akka.stream.alpakka.google.implicits._

final case class GoogleHttpException() extends Exception

object GoogleHttpException {
  implicit val exceptionUnmarshaller: FromResponseUnmarshaller[Throwable] = Unmarshaller.withMaterializer {
    implicit ec => implicit mat => (r: HttpResponse) =>
      r.discardEntityBytes().future().map(_ => GoogleHttpException(): Throwable)
  }.withDefaultRetry
}
