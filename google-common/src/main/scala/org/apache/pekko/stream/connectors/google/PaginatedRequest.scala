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

package org.apache.pekko.stream.connectors.google

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.dispatch.ExecutionContexts
import pekko.http.scaladsl.model.HttpMethods.GET
import pekko.http.scaladsl.model.HttpRequest
import pekko.http.scaladsl.model.Uri.Query
import pekko.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import pekko.stream.connectors.google.http.GoogleHttp
import pekko.stream.connectors.google.scaladsl.Paginated
import pekko.stream.scaladsl.Source
import pekko.{ Done, NotUsed }

import scala.concurrent.Future

@InternalApi
private[connectors] object PaginatedRequest {

  private val futureNone = Future.successful(None)

  /**
   * Makes a series of authenticated requests to page through a resource.
   * Requests are retried if the unmarshaller throws a [[pekko.stream.connectors.google.util.Retry]].
   *
   * @param request the [[HttpRequest]] to make; must be a GET request
   * @tparam Out the data model for each page of the resource
   * @return a [[pekko.stream.scaladsl.Source]] that emits an `Out` for each page of the resource
   */
  def apply[Out: FromResponseUnmarshaller](request: HttpRequest)(
      implicit paginated: Paginated[Out]): Source[Out, NotUsed] = {

    require(request.method == GET, "Paginated request must be GET request")

    val parsedQuery = request.uri.query()
    val initialPageToken = parsedQuery.get("pageToken")
    val query = parsedQuery.filterNot(_._1 == "pageToken")

    Source
      .fromMaterializer { (mat, attr) =>
        implicit val system = mat.system
        implicit val settings = GoogleAttributes.resolveSettings(mat, attr)

        val requestWithPageToken = addPageToken(request, query)
        Source.unfoldAsync[Either[Done, Option[String]], Out](Right(initialPageToken)) {
          case Left(Done) => futureNone

          case Right(pageToken) =>
            val updatedRequest = pageToken.fold(request)(requestWithPageToken)
            GoogleHttp()
              .singleAuthenticatedRequest(updatedRequest)
              .map { out =>
                val nextPageToken = paginated
                  .pageToken(out)
                  .fold[Either[Done, Option[String]]](Left(Done))(pageToken => Right(Some(pageToken)))
                Some((nextPageToken, out))
              }(ExecutionContexts.parasitic)
        }
      }
      .mapMaterializedValue(_ => NotUsed)
  }

  private def addPageToken(request: HttpRequest, query: Query): String => HttpRequest = { pageToken =>
    request.withUri(request.uri.withQuery(Query.Cons("pageToken", pageToken, query)))
  }
}
