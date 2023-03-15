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

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.dispatch.ExecutionContexts
import pekko.http.scaladsl.model.headers.OAuth2BearerToken
import pekko.stream.connectors.google.RequestSettings
import pekko.stream.connectors.google.auth.OAuth2Credentials.{ ForceRefresh, TokenRequest }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.{ CompletionStrategy, Materializer, OverflowStrategy }
import com.google.auth.{ Credentials => GoogleCredentials }

import java.time.Clock
import scala.concurrent.{ ExecutionContext, Future, Promise }

@InternalApi
private[auth] object OAuth2Credentials {
  sealed abstract class Command
  final case class TokenRequest(promise: Promise[OAuth2BearerToken], settings: RequestSettings) extends Command
  final case object ForceRefresh extends Command
}

@InternalApi
private[auth] abstract class OAuth2Credentials(val projectId: String)(implicit mat: Materializer) extends Credentials {

  private val tokenStream = stream.run()

  override def get()(implicit ec: ExecutionContext, settings: RequestSettings): Future[OAuth2BearerToken] = {
    val token = Promise[OAuth2BearerToken]()
    tokenStream ! TokenRequest(token, settings)
    token.future
  }

  def refresh(): Unit = tokenStream ! ForceRefresh

  override def asGoogle(implicit ec: ExecutionContext, settings: RequestSettings): GoogleCredentials =
    new GoogleOAuth2Credentials(this)(ec, settings)

  protected def getAccessToken()(implicit mat: Materializer,
      settings: RequestSettings,
      clock: Clock): Future[AccessToken]

  private def stream =
    Source
      .actorRef[OAuth2Credentials.Command](
        PartialFunction.empty[Any, CompletionStrategy],
        PartialFunction.empty[Any, Throwable],
        Int.MaxValue,
        OverflowStrategy.fail)
      .to(
        Sink.fromMaterializer { (mat, attr) =>
          Sink.foldAsync(Option.empty[AccessToken]) {
            case (cachedToken @ Some(token), TokenRequest(promise, _)) if !token.expiresSoon()(Clock.systemUTC()) =>
              promise.success(OAuth2BearerToken(token.token))
              Future.successful(cachedToken)
            case (_, TokenRequest(promise, settings)) =>
              getAccessToken()(mat, settings, Clock.systemUTC())
                .andThen {
                  case response =>
                    promise.complete(response.map(t => OAuth2BearerToken(t.token)))
                }(ExecutionContexts.parasitic)
                .map(Some(_))(ExecutionContexts.parasitic)
                .recover { case _ => None }(ExecutionContexts.parasitic)
            case (_, ForceRefresh) =>
              Future.successful(None)
          }
        })
}
