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

package org.apache.pekko.stream.connectors.couchbase.impl

import com.couchbase.client.java.{ AsyncBucket, AsyncCluster, AsyncCollection, AsyncScope, Collection }
import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.couchbase.{ javadsl, scaladsl }
import org.apache.pekko.util.FutureConverters._

import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[couchbase] final class CouchbaseSessionScalaAdapter(delegate: javadsl.CouchbaseSession)
    extends scaladsl.CouchbaseSession {

  override def asJava: javadsl.CouchbaseSession = delegate

  override def underlying: AsyncCluster = delegate.underlying

  /**
   * Close the session and release all resources it holds. Subsequent calls to other methods will likely fail.
   */
  override def close(): Future[Done] =
    delegate.close().asScala

  override def bucket(bucketName: String): AsyncBucket = delegate.bucket(bucketName)

  override def scope(bucketName: String): AsyncScope = delegate.scope(bucketName)

  override def scope(bucketName: String, scopeName: String): AsyncScope = delegate.scope(bucketName, scopeName)

  override def collection(bucketName: String): AsyncCollection = delegate.collection(bucketName)

  override def collection(bucketName: String, collectionName: String): AsyncCollection =
    delegate.collection(bucketName, collectionName)

  override def collection(bucketName: String, scopeName: String, collectionName: String): AsyncCollection =
    delegate.collection(bucketName, scopeName, collectionName)

  override def get[T](bucketName: String, id: String, target: Class[T]): Future[T] =
    delegate.get(bucketName, id, target).asScala
}
