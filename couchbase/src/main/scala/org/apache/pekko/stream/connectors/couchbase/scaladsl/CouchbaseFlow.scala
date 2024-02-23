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

import com.couchbase.client.java.json.{ JsonObject, JsonValue }
import com.couchbase.client.java.kv.{ GetOptions, GetResult, MutationResult }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.couchbase._
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.FutureConverters.CompletionStageOps

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Scala API: Factory methods for Couchbase flows.
 */
object CouchbaseFlow {

  /**
   * Create a flow to query Couchbase for by `id` and emit [[com.couchbase.client.java.kv.GetResult]].
   */
  def fromId(sessionSettings: CouchbaseSessionSetting, bucketName: String): Flow[String, GetResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[String]
          .mapAsync(1)(id =>
            session.flatMap(_.collection(bucketName)
              .get(id, GetOptions.getOptions)
              .asScala)(materializer.system.dispatcher))
      }.mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to query Couchbase for by `id` and emit documents of the given class.
   */
  def fromId[T](sessionSettings: CouchbaseSessionSetting,
      bucketName: String,
      target: Class[T]): Flow[String, T, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[String]
          .mapAsync(1)(id =>
            session.flatMap(_.collection(bucketName)
              .get(id, GetOptions.getOptions)
              .thenApply(_.contentAs(target))
              .asScala)(materializer.system.dispatcher))
      }.mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to update or insert a Couchbase [[com.couchbase.client.java.kv.MutationResult]].
   */
  def upsertJson(sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonObject => String): Flow[JsonObject, MutationResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[JsonObject]
          .mapAsync(writeSettings.parallelism)(doc =>
            session.flatMap(_.collection(bucketName)
              .upsert(getId(doc), doc, writeSettings.toUpsertOption)
              .asScala)(materializer.system.dispatcher))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to update or insert a Couchbase document of the given class.
   */
  def upsert[T](sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, MutationResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[T]
          .mapAsync(writeSettings.parallelism)(doc =>
            session.flatMap(_.collection(bucketName)
              .upsert(getId(doc), doc, writeSettings.toUpsertOption)
              .asScala)(materializer.system.dispatcher))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to update or insert a Couchbase document of the given class and emit a result so that write failures
   * can be handled in-stream.
   */
  def upsertWithResult[T](sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, CouchbaseWriteResult[T], NotUsed] = {

    val flow: Flow[T, CouchbaseWriteResult[T], Future[NotUsed]] = Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[T]
          .mapAsync(writeSettings.parallelism)(doc => {
            val id = getId(doc)
            implicit val executor: ExecutionContext = materializer.system.dispatcher
            session
              .flatMap(_.collection(bucketName)
                .upsert(getId(doc), doc, writeSettings.toUpsertOption)
                .asScala
                .map(result => CouchbaseWriteSuccess(id, doc, result))
                .recover {
                  case exception => CouchbaseWriteFailure(id, doc, exception)
                })
          })
      }
    flow.mapMaterializedValue(_ => NotUsed)
  }

  /**
   * Create a flow to replace a Couchbase [[com.couchbase.client.java.json.JsonValue]].
   */
  def replaceJson(sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: JsonValue => String): Flow[JsonValue, MutationResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[JsonValue]
          .mapAsync(writeSettings.parallelism)(doc =>
            session.flatMap(_.collection(bucketName)
              .replace(getId(doc), doc, writeSettings.toReplaceOption)
              .asScala)(materializer.system.dispatcher))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to replace a Couchbase document of the given class.
   */
  def replace[T](sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, MutationResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[T]
          .mapAsync(writeSettings.parallelism)(doc =>
            session.flatMap(_.collection(bucketName).replace(getId(doc), doc,
              writeSettings.toReplaceOption).asScala)(
              materializer.system.dispatcher))
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to replace a Couchbase document of the given class and emit a result so that write failures
   * can be handled in-stream.
   */
  def replaceWithResult[T](sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String,
      getId: T => String): Flow[T, CouchbaseWriteResult[T], NotUsed] = {
    val flow: Flow[T, CouchbaseWriteResult[T], Future[NotUsed]] = Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[T]
          .mapAsync(writeSettings.parallelism)(doc => {
            val id = getId(doc)
            implicit val executor: ExecutionContext = materializer.system.dispatcher
            session
              .flatMap(_.collection(bucketName).replace(getId(doc), doc, writeSettings.toReplaceOption).asScala)
              .map(res => CouchbaseWriteSuccess(id, doc, res))
              .recover {
                case exception => CouchbaseWriteFailure(id, doc, exception)
              }
          })
      }
    flow.mapMaterializedValue(_ => NotUsed)
  }

  /**
   * Create a flow to delete documents from Couchbase by `id`. Emits the same `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Flow[String, String, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[String]
          .mapAsync(writeSettings.parallelism)(id => {
            implicit val executor: ExecutionContext = materializer.system.dispatcher
            session
              .flatMap(_.collection(bucketName).remove(id, writeSettings.toRemoveOption).asScala)
              .map(_ => id)
          })
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to delete documents from Couchbase by `id` and emit operation outcome containing the same `id`.
   */
  def deleteWithResult(sessionSettings: CouchbaseSessionSetting,
      writeSettings: CouchbaseWriteSettings,
      bucketName: String): Flow[String, CouchbaseDeleteResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings)
        Flow[String]
          .mapAsync(writeSettings.parallelism)(id => {
            implicit val executor: ExecutionContext = materializer.system.dispatcher
            session
              .flatMap(_.collection(bucketName).remove(id, writeSettings.toRemoveOption).asScala)
              .map(result => CouchbaseDeleteSuccess(id, result))
              .recover {
                case exception => CouchbaseDeleteFailure(id, exception)
              }
          })
      }.mapMaterializedValue(_ => NotUsed)
}
