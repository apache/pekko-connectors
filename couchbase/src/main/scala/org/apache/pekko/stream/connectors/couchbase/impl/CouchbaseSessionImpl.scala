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

import com.couchbase.client.java._
import com.couchbase.client.java.json.JsonObject
import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.couchbase.javadsl.CouchbaseSession
import org.apache.pekko.stream.connectors.couchbase.scaladsl

import java.util.concurrent.{ CompletableFuture, CompletionStage }
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * INTERNAL API
 */
@InternalApi
final private[couchbase] class CouchbaseSessionImpl(cluster: AsyncCluster)
    extends CouchbaseSession {

  override def asScala: scaladsl.CouchbaseSession = new CouchbaseSessionScalaAdapter(this)

  override def close(): CompletionStage[Done] =
    cluster.disconnect().thenApply(_ => Done.done())

  override def underlying = cluster

  override def bucket(bucketName: String): AsyncBucket = cluster.bucket(bucketName)

  override def scope(bucketName: String): AsyncScope = bucket(bucketName).defaultScope()

  override def scope(bucketName: String, scopeName: String): AsyncScope = bucket(bucketName).scope(scopeName)

  override def collection(bucketName: String): AsyncCollection = bucket(bucketName).defaultCollection()

  override def collection(bucketName: String, collectionName: String): AsyncCollection =
    bucket(bucketName).collection(collectionName)

  override def collection(bucketName: String, scopeName: String, collectionName: String): AsyncCollection =
    bucket(bucketName).scope(scopeName).collection(collectionName)

  override def get[T](collection: AsyncCollection, id: String, target: Class[T]): CompletableFuture[T] =
    collection.get(id).thenApply(_.contentAs(target))

  override def getJson(collection: AsyncCollection, id: String): CompletableFuture[JsonObject] =
    collection.get(id).thenApply(_.contentAsObject())

}
