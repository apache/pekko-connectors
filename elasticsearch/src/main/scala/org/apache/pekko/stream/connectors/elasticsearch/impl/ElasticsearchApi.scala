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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.http.scaladsl.HttpExt
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.headers.BasicHttpCredentials
import pekko.stream.connectors.elasticsearch.ElasticsearchConnectionSettings

import scala.concurrent.Future

@InternalApi private[impl] object ElasticsearchApi {
  def executeRequest(
      request: HttpRequest,
      connectionSettings: ElasticsearchConnectionSettings)(implicit http: HttpExt): Future[HttpResponse] = {
    if (connectionSettings.hasCredentialsDefined) {
      http.singleRequest(
        request.addCredentials(BasicHttpCredentials(connectionSettings.username.get, connectionSettings.password.get)))
    } else {
      http.singleRequest(request,
        connectionContext =
          connectionSettings.connectionContext.getOrElse(http.defaultClientHttpsContext))
    }
  }
}
