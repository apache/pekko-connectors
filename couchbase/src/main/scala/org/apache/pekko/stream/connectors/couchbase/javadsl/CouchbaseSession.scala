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

package org.apache.pekko.stream.connectors.couchbase.javadsl

import com.couchbase.client.java.{ AsyncCluster, AsyncCollection }
import org.apache.pekko
import org.apache.pekko.Done
import org.apache.pekko.stream.connectors.couchbase.impl.{ CouchbaseSessionCommon, CouchbaseSessionImpl }
import pekko.annotation.DoNotInherit
import pekko.stream.connectors.couchbase.CouchbaseSessionSetting
import pekko.stream.connectors.couchbase.scaladsl.{ CouchbaseSession => ScalaDslCouchbaseSession }
import pekko.util.FutureConverters._

import java.util.concurrent.{ CompletableFuture, CompletionStage, Executor }
import scala.concurrent.ExecutionContext

/**
 * Java API: Gives access to Couchbase.
 *
 * @see [[pekko.stream.connectors.couchbase.CouchbaseSessionRegistry]]
 */
object CouchbaseSession {

  /**
   * Create a session against the given bucket. The couchbase client used to connect will be created and then closed when
   * the session is closed.
   */
  def create(settings: CouchbaseSessionSetting, executor: Executor): CompletionStage[CouchbaseSession] = {
    ScalaDslCouchbaseSession
      .createClusterClient(settings)(executionContext(executor))
      .map(create)(executionContext(executor))
      .asJava
  }

  /**
   * Connects to a Couchbase cluster by creating an `AsyncCluster`.
   * The life-cycle of it is the user's responsibility.
   */
  def createClient(settings: CouchbaseSessionSetting, executor: Executor): CompletionStage[AsyncCluster] = {
    ScalaDslCouchbaseSession
      .createClusterClient(settings)(executionContext(executor))
      .asJava
  }

  /**
   * Create a given bucket using a pre-existing cluster client, allowing for it to be shared among
   * multiple `CouchbaseSession`s. The cluster client's life-cycle is the user's responsibility.
   */
  def create(cluster: AsyncCluster): CouchbaseSession =
    new CouchbaseSessionImpl(cluster)

  private def executionContext(executor: Executor): ExecutionContext =
    executor match {
      case ec: ExecutionContext => ec
      case _                    => ExecutionContext.fromExecutor(executor)
    }

}

/**
 * Java API: A Couchbase session allowing querying and interacting with a specific couchbase bucket.
 *
 * Not for user extension.
 */
// must be an abstract class, otherwise static forwarders are missing for companion object if building with Scala 2.11
@DoNotInherit
abstract class CouchbaseSession extends CouchbaseSessionCommon {

  def asScala: ScalaDslCouchbaseSession

  def close(): CompletionStage[Done]

  def get[T](collection: AsyncCollection, id: String, target: Class[T]): CompletableFuture[T]
}
