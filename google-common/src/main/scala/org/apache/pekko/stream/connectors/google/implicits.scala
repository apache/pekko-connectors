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
import pekko.annotation.InternalApi
import pekko.http.scaladsl.model.HttpResponse
import pekko.http.scaladsl.model.StatusCodes._
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, FromResponseUnmarshaller, Unmarshal, Unmarshaller }
import pekko.http.scaladsl.util.FastFuture.EnhancedFuture
import pekko.stream.connectors.google.util.Retry

@InternalApi
private[connectors] object implicits {

  implicit final class QueryPrependOption(val query: Query) extends AnyVal {
    def ?+:(kv: (String, Option[Any])): Query = kv._2.fold(query)(v => Query.Cons(kv._1, v.toString, query))
  }

  implicit final class FromResponseUnmarshallerRetryHelpers[T](val um: FromResponseUnmarshaller[T]) extends AnyVal {

    /**
     * Automatically identifies retryable exceptions using reasonable defaults. Should be sufficient for most APIs.
     */
    def withDefaultRetry: FromResponseUnmarshaller[T] =
      Unmarshaller.withMaterializer { implicit ec => implicit mat => response =>
        um(response).recoverWith {
          case ex =>
            Unmarshaller.strict((_: HttpResponse) => ex).withDefaultRetry.apply(response).fast.map(throw _)
        }
      }

    /**
     * Disables all retries
     */
    def withoutRetries: FromResponseUnmarshaller[T] = um.recover { _ => _ =>
      {
        case Retry(ex) => throw ex
      }
    }
  }

  implicit final class ToThrowableUnmarshallerRetryHelpers(val um: FromResponseUnmarshaller[Throwable]) extends AnyVal {

    /**
     * Automatically identifies retryable exceptions using reasonable defaults. Should be sufficient for most APIs.
     */
    def withDefaultRetry: FromResponseUnmarshaller[Throwable] =
      Unmarshaller.withMaterializer { implicit ec => implicit mat => response =>
        um(response).map {
          case ex =>
            response.status match {
              case TooManyRequests | InternalServerError | BadGateway | ServiceUnavailable | GatewayTimeout => Retry(ex)
              case _                                                                                        => ex
            }
        }
      }
  }

  /**
   * Merges a success and failure unmarshaller into a single unmarshaller
   */
  implicit def responseUnmarshallerWithExceptions[T](
      implicit um: FromEntityUnmarshaller[T],
      exUm: FromResponseUnmarshaller[Throwable]): FromResponseUnmarshaller[T] =
    Unmarshaller.withMaterializer { implicit ec => implicit mat => response =>
      if (response.status.isSuccess())
        Unmarshal(response.entity).to[T]
      else
        Unmarshal(response).to[Throwable].map(throw _)
    }
}
