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

package org.apache.pekko.stream.connectors.couchbase.scaladsl

import com.couchbase.client.java._
import org.apache.pekko
import org.apache.pekko.Done
import org.apache.pekko.annotation.{ DoNotInherit, InternalApi }
import org.apache.pekko.stream.connectors.couchbase.impl.{ CouchbaseSessionCommon, CouchbaseSessionImpl }
import org.apache.pekko.stream.connectors.couchbase.{ javadsl, CouchbaseSessionSettings }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Scala API: Gives access to Couchbase.
 *
 * @see [[pekko.stream.connectors.couchbase.CouchbaseSessionRegistry]]
 */
object CouchbaseSession {

  def apply(settings: CouchbaseSessionSettings)(
      implicit ec: ExecutionContext): Future[CouchbaseSession] =
    createClusterClient(settings).map(c => create(c))

  /**
   * Create a given bucket using a pre-existing cluster client, allowing for it to be shared among
   * multiple `CouchbaseSession`s. The cluster client's life-cycle is the user's responsibility.
   */
  def apply(cluster: AsyncCluster): CouchbaseSession =
    create(cluster)

  /**
   * Create a given bucket using a pre-existing cluster client, allowing for it to be shared among
   * multiple `CouchbaseSession`s. The cluster client's life-cycle is the user's responsibility.
   */
  @InternalApi
  private[couchbase] def create(cluster: AsyncCluster): CouchbaseSession = {
    new CouchbaseSessionImpl(cluster).asScala
  }

  /**
   * INTERNAL API.
   *
   * Connects to a Couchbase cluster by creating an `AsyncCluster`.
   * The life-cycle of it is the user's responsibility.
   */
  @InternalApi
  private[couchbase] def createClusterClient(settings: CouchbaseSessionSettings)(
      implicit ec: ExecutionContext): Future[AsyncCluster] =
    settings.enriched
      .flatMap { enrichedSettings =>
        Future(enrichedSettings.environment match {
          case Some(environment) =>
            ClusterOptions
              .clusterOptions(enrichedSettings.username, enrichedSettings.username)
              .environment(environment)
          case None =>
            ClusterOptions.clusterOptions(enrichedSettings.username, enrichedSettings.username)
        }).map { clusterOptions =>
          Cluster.connect(enrichedSettings.nodes.mkString(","), clusterOptions).async()
        }
      }

}

/**
 * Scala API: A Couchbase session allowing querying and interacting with a specific couchbase bucket.
 *
 * Not for user extension.
 */
@DoNotInherit
trait CouchbaseSession extends CouchbaseSessionCommon {

  def asJava: javadsl.CouchbaseSession

  /**
   * Close the session and release all resources it holds. Subsequent calls to other methods will likely fail.
   */
  def close(): Future[Done]

  def get[T](bucketName: String, id: String, target: Class[T]): Future[T]
}
