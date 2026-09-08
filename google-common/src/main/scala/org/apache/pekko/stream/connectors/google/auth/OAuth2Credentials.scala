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

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.http.scaladsl.model.headers.OAuth2BearerToken
import pekko.stream.connectors.google.RequestSettings
import pekko.stream.connectors.google.auth.OAuth2Credentials.{ Close, ForceRefresh, TokenRequest }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.stream.{ CompletionStrategy, Materializer, OverflowStrategy }
import com.google.auth.{ Credentials => GoogleCredentials }

import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ ExecutionContext, Future, Promise }

@InternalApi
private[auth] object OAuth2Credentials {
  sealed abstract class Command
  final case class TokenRequest(promise: Promise[OAuth2BearerToken], settings: RequestSettings) extends Command
  case object ForceRefresh extends Command
  case object Close extends Command
}

@InternalApi
private[auth] abstract class OAuth2Credentials(val projectId: String)(implicit mat: Materializer) extends Credentials
    with RetrievableCredentials {

  private[auth] val tokenStream = stream.run()
  private val closed = new AtomicBoolean(false)

  override def get()(implicit ec: ExecutionContext, settings: RequestSettings): Future[OAuth2BearerToken] =
    if (closed.get())
      Future.failed(new IllegalStateException("These credentials have been closed"))
    else {
      val token = Promise[OAuth2BearerToken]()
      tokenStream ! TokenRequest(token, settings)
      token.future
    }

  def refresh(): Unit = if (!closed.get()) tokenStream ! ForceRefresh

  /**
   * Completes the token stream, releasing the actor and the materialized stages behind it. Requests
   * already queued are still served; requests made after closing fail with an
   * [[java.lang.IllegalStateException]].
   */
  override def close(): Unit = if (closed.compareAndSet(false, true)) tokenStream ! Close

  override def asGoogle(implicit ec: ExecutionContext, settings: RequestSettings): GoogleCredentials =
    new GoogleOAuth2Credentials(this)(ec, settings)

  protected def getAccessToken()(implicit mat: Materializer,
      settings: RequestSettings,
      clock: Clock): Future[AccessToken]

  private def stream =
    Source
      .actorRef[OAuth2Credentials.Command](
        { case Close => CompletionStrategy.draining },
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
                }(ExecutionContext.parasitic)
                .map(Some(_))(ExecutionContext.parasitic)
                .recover { case _ => None }(ExecutionContext.parasitic)
            case (_, ForceRefresh) =>
              Future.successful(None)
            case (cachedToken, Close) =>
              // consumed by the completion matcher above, here only to keep the match exhaustive
              Future.successful(cachedToken)
          }
        })
}
