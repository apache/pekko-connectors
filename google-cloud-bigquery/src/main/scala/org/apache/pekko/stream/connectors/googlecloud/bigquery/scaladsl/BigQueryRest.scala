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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import org.apache.pekko.stream.connectors.google.scaladsl.Google
import org.apache.pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import org.apache.pekko.stream.scaladsl.Source

import scala.concurrent.Future

private[scaladsl] trait BigQueryRest extends Google {

  // Helper methods

  protected[this] def source[Out, Mat](f: GoogleSettings => Source[Out, Mat]): Source[Out, Future[Mat]] =
    Source.fromMaterializer { (mat, attr) =>
      f(GoogleAttributes.resolveSettings(mat, attr))
    }

  protected[this] def mkFilterParam(filter: Map[String, String]): String =
    filter.view
      .map {
        case (key, value) =>
          val colonValue = if (value.isEmpty) "" else s":$value"
          s"label.$key$colonValue"
      }
      .mkString(" ")

  protected[this] implicit val doneUnmarshaller: FromEntityUnmarshaller[Done] =
    Unmarshaller.withMaterializer(_ => implicit mat => _.discardBytes().future)
}
