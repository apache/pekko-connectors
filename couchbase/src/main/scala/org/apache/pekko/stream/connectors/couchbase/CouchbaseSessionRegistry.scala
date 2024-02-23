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

package org.apache.pekko.stream.connectors.couchbase

import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicReference
import org.apache.pekko
import pekko.actor.{ ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import pekko.dispatch.ExecutionContexts
import pekko.stream.connectors.couchbase.impl.CouchbaseClusterRegistry
import pekko.stream.connectors.couchbase.javadsl.{ CouchbaseSession => JCouchbaseSession }
import pekko.stream.connectors.couchbase.scaladsl.CouchbaseSession
import pekko.util.FutureConverters._

import scala.annotation.tailrec
import scala.concurrent.{ Future, Promise }

/**
 * This Couchbase session registry makes it possible to share Couchbase sessions between multiple use sites
 * in the same `ActorSystem` (important for the Couchbase Pekko Persistence plugin where it is shared between journal,
 * query plugin and snapshot plugin)
 */
object CouchbaseSessionRegistry extends ExtensionId[CouchbaseSessionRegistry] with ExtensionIdProvider {
  def createExtension(system: ExtendedActorSystem): CouchbaseSessionRegistry =
    new CouchbaseSessionRegistry(system)

  /**
   * Java API: Get the session registry with new actors API.
   */
  override def get(system: ClassicActorSystemProvider): CouchbaseSessionRegistry =
    super.apply(system)

  /**
   * Java API: Get the session registry with the classic actors API.
   */
  override def get(system: pekko.actor.ActorSystem): CouchbaseSessionRegistry =
    super.apply(system)

  override def lookup: ExtensionId[CouchbaseSessionRegistry] = this

  private case class SessionKey(settings: CouchbaseSessionSettings)
}

final class CouchbaseSessionRegistry(system: ExtendedActorSystem) extends Extension {

  import CouchbaseSessionRegistry._

  private val blockingDispatcher = system.dispatchers.lookup("pekko.actor.default-blocking-io-dispatcher")

  private val clusterRegistry = new CouchbaseClusterRegistry(system)

  private val sessions = new AtomicReference(Map.empty[SessionKey, Future[CouchbaseSession]])

  /**
   * Scala API: Get an existing session or start a new one with the given settings and bucket name,
   * makes it possible to share one session across plugins.
   *
   * Note that the session must not be stopped manually, it is shut down when the actor system is shutdown,
   * if you need a more fine grained life cycle control, create the CouchbaseSession manually instead.
   */
  def sessionFor(settings: CouchbaseSessionSettings): Future[CouchbaseSession] =
    settings.enriched.flatMap { enrichedSettings =>
      val key = SessionKey(enrichedSettings)
      sessions.get.get(key) match {
        case Some(futureSession) => futureSession
        case _                   => startSession(key)
      }
    }(system.dispatcher)

  /**
   * Java API: Get an existing session or start a new one with the given settings and bucket name,
   * makes it possible to share one session across plugins.
   *
   * Note that the session must not be stopped manually, it is shut down when the actor system is shutdown,
   * if you need a more fine grained life cycle control, create the CouchbaseSession manually instead.
   */
  def getSessionFor(settings: CouchbaseSessionSettings): CompletionStage[JCouchbaseSession] =
    sessionFor(settings)
      .map(_.asJava)(ExecutionContexts.parasitic)
      .asJava

  @tailrec
  private def startSession(key: SessionKey): Future[CouchbaseSession] = {
    val promise = Promise[CouchbaseSession]()
    val oldSessions = sessions.get()
    val newSessions = oldSessions.updated(key, promise.future)
    if (sessions.compareAndSet(oldSessions, newSessions)) {
      // we won cas, initialize session
      val session = clusterRegistry
        .clusterFor(key.settings)
        .map(cluster => CouchbaseSession(cluster))(
          ExecutionContexts.parasitic)
      promise.completeWith(session)
      promise.future
    } else {
      // we lost cas (could be concurrent call for some other key though), retry
      startSession(key)
    }
  }

}
