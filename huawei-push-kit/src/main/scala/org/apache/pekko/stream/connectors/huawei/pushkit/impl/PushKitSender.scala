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

package org.apache.pekko.stream.connectors.huawei.pushkit.impl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.huawei.pushkit.ForwardProxyHttpsContext.ForwardProxyHttpsContext
import org.apache.pekko.stream.connectors.huawei.pushkit.ForwardProxyPoolSettings.ForwardProxyPoolSettings
import org.apache.pekko.stream.connectors.huawei.pushkit.HmsSettings
import org.apache.pekko.stream.connectors.huawei.pushkit.models.{ ErrorResponse, PushKitResponse, Response }
import spray.json.enrichAny

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

/**
 * INTERNAL API
 */
@InternalApi
private[pushkit] class PushKitSender {
  import PushKitJsonSupport._

  def send(conf: HmsSettings, token: String, http: HttpExt, hmsSend: PushKitSend, system: ActorSystem)(
      implicit materializer: Materializer): Future[Response] = {
    val appId = conf.appId
    val forwardProxy = conf.forwardProxy
    val url = s"https://push-api.cloud.huawei.com/v1/$appId/messages:send"

    val response = forwardProxy match {
      case Some(fp) =>
        http.singleRequest(
          HttpRequest(
            HttpMethods.POST,
            url,
            immutable.Seq(Authorization(OAuth2BearerToken(token))),
            HttpEntity(ContentTypes.`application/json`, hmsSend.toJson.compactPrint)),
          connectionContext = fp.httpsContext(system),
          settings = fp.poolSettings(system))
      case None =>
        http.singleRequest(
          HttpRequest(
            HttpMethods.POST,
            url,
            immutable.Seq(Authorization(OAuth2BearerToken(token))),
            HttpEntity(ContentTypes.`application/json`, hmsSend.toJson.compactPrint)))
    }
    parse(response)
  }

  private def parse(response: Future[HttpResponse])(implicit materializer: Materializer): Future[Response] = {
    implicit val executionContext: ExecutionContext = materializer.executionContext
    response.flatMap { rsp =>
      if (rsp.status.isSuccess) {
        Unmarshal(rsp.entity).to[PushKitResponse]
      } else {
        Unmarshal(rsp.entity).to[ErrorResponse]
      }
    }
  }
}
