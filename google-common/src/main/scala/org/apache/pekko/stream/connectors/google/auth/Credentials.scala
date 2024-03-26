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
import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.DoNotInherit
import pekko.event.Logging
import pekko.http.scaladsl.model.headers.HttpCredentials
import pekko.stream.connectors.google.RequestSettings
import pekko.util.JavaDurationConverters._
import pekko.util.ccompat.JavaConverters._
import com.google.auth.{ Credentials => GoogleCredentials }
import com.typesafe.config.Config

import java.util.concurrent.Executor
import scala.collection.immutable.ListMap
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.control.NonFatal

object Credentials {

  /**
   * Creates [[Credentials]] to access Google APIs from a given configuration.
   * Assume that construction is "resource-heavy" (e.g. spawns actors) so prefer to cache and reuse.
   */
  def apply(c: Config)(implicit system: ClassicActorSystemProvider): Credentials = c.getString("provider") match {
    case "application-default" =>
      val log = Logging(system.classicSystem, classOf[Credentials])
      try {
        val creds = parseServiceAccount(c)
        log.info("Using service account credentials")
        creds
      } catch {
        case NonFatal(ex1) =>
          try {
            val creds = parseComputeEngine(c)
            log.info("Using Compute Engine credentials")
            creds
          } catch {
            case NonFatal(ex2) =>
              try {
                val creds = parseUserAccess(c)
                log.info("Using user access credentials")
                creds
              } catch {
                case NonFatal(ex3) =>
                  log.warning("Unable to find Application Default Credentials for Google APIs")
                  log.warning("Service account: {}", ex1.getMessage)
                  log.warning("Compute Engine: {}", ex2.getMessage)
                  log.warning("User access: {}", ex3.getMessage)
                  parseNone(c) // TODO Once credentials are guaranteed to be managed centrally we can throw an error instead
              }
          }
      }
    case "service-account" => parseServiceAccount(c)
    case "compute-engine"  => parseComputeEngine(c)
    case "user-access"     => parseUserAccess(c)
    case "none"            => parseNone(c)
  }

  private def parseServiceAccount(c: Config)(implicit system: ClassicActorSystemProvider) = {
    val scopes = c.getStringList("scopes").asScala.toSet
    ServiceAccountCredentials(c.getConfig("service-account"), scopes)
  }

  private def parseComputeEngine(c: Config)(implicit system: ClassicActorSystemProvider) = {
    val scopes = c.getStringList("scopes").asScala.toSet
    Await.result(ComputeEngineCredentials(scopes), c.getDuration("compute-engine.timeout").asScala)
  }

  private def parseUserAccess(c: Config)(implicit system: ClassicActorSystemProvider) =
    UserAccessCredentials(c.getConfig("user-access"))

  private def parseNone(c: Config) = NoCredentials(c.getConfig("none"))

  private var _cache: Map[Any, Credentials] = ListMap.empty
  @deprecated("Intended only to help with migration", "Alpakka 3.0.0")
  private[connectors] def cache(key: Any)(default: => Credentials) =
    _cache.getOrElse(key, {
        val credentials = default
        _cache += (key -> credentials)
        credentials
      })

}

/**
 * Credentials for accessing Google APIs
 */
@DoNotInherit
abstract class Credentials private[auth] {

  private[google] def projectId: String

  private[google] def get()(implicit ec: ExecutionContext, settings: RequestSettings): Future[HttpCredentials]

  /**
   * Wraps these credentials as a [[com.google.auth.Credentials]] for interop with Google's Java client libraries.
   * @param ec the [[scala.concurrent.ExecutionContext]] to use for blocking requests if credentials are requested synchronously
   * @param settings additional request settings
   */
  def asGoogle(implicit ec: ExecutionContext, settings: RequestSettings): GoogleCredentials

  /**
   * Java API: Wraps these credentials as a [[com.google.auth.Credentials]] for interop with Google's Java client libraries.
   * @param exec the [[java.util.concurrent.Executor]] to use for blocking requests if credentials are requested synchronously
   * @param settings additional request settings
   */
  final def asGoogle(exec: Executor, settings: RequestSettings): GoogleCredentials =
    asGoogle(ExecutionContext.fromExecutor(exec): ExecutionContext, settings)
}
