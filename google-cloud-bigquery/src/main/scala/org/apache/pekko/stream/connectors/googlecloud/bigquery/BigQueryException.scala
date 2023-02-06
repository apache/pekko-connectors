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

package org.apache.pekko.stream.connectors.googlecloud.bigquery

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes.Forbidden
import org.apache.pekko.http.scaladsl.model.{ ErrorInfo, ExceptionWithErrorInfo, HttpResponse }
import org.apache.pekko.http.scaladsl.unmarshalling.{
  FromResponseUnmarshaller,
  PredefinedFromEntityUnmarshallers,
  Unmarshaller
}
import org.apache.pekko.stream.connectors.google.implicits._
import org.apache.pekko.stream.connectors.google.util.Retry
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.ErrorProto
import spray.json.DefaultJsonProtocol._
import spray.json.{ enrichAny, RootJsonFormat }

import scala.annotation.nowarn

final case class BigQueryException private (override val info: ErrorInfo, raw: String)
    extends ExceptionWithErrorInfo(info) {
  def getInfo = info
  def getRaw = raw
}

object BigQueryException {

  private[bigquery] def apply(message: String): BigQueryException =
    BigQueryException(ErrorInfo(message), message)

  implicit val fromResponseUnmarshaller: FromResponseUnmarshaller[Throwable] =
    Unmarshaller
      .withMaterializer { implicit ec => implicit mat => response: HttpResponse =>
        import SprayJsonSupport._
        val HttpResponse(status, _, entity, _) = response: @nowarn("msg=match may not be exhaustive")
        Unmarshaller
          .firstOf(
            sprayJsValueUnmarshaller.map { json =>
              json
                .convertTo[ErrorResponse]
                .error
                .fold[Throwable](BigQueryException(ErrorInfo(status.value, status.defaultMessage), json.prettyPrint)) {
                  case ErrorProto(reason, _, message) =>
                    val summary = reason.fold(status.value)(reason => s"${response.status.intValue} $reason")
                    val detail = message.getOrElse("")
                    BigQueryException(ErrorInfo(summary, detail), json.prettyPrint)
                }
            },
            PredefinedFromEntityUnmarshallers.stringUnmarshaller.map { error =>
              BigQueryException(ErrorInfo(status.value, error), error): Throwable
            })
          .apply(entity)
      }
      .withDefaultRetry
      .mapWithInput {
        case (HttpResponse(Forbidden, _, _, _), ex @ BigQueryException(ErrorInfo(summary, _), _))
            if summary.contains("rateLimitExceeded") =>
          Retry(ex)
        case (_, ex) => ex
      }

  private[bigquery] def apply(error: ErrorProto): BigQueryException = error match {
    case ErrorProto(reason, location, message) =>
      val at = location.fold("")(loc => s" at $loc")
      val summary = reason.fold("")(reason => s"$reason$at")
      val info = ErrorInfo(summary, message.getOrElse(""))
      BigQueryException(info, error.toJson.prettyPrint)
  }

  private final case class ErrorResponse(error: Option[ErrorProto])
  private implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse)
}
