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

package org.apache.pekko.stream.connectors.elasticsearch.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.elasticsearch.{ scaladsl, _ }
import com.fasterxml.jackson.databind.ObjectMapper

import scala.collection.JavaConverters._

/**
 * Java API to create Elasticsearch flows.
 */
object ElasticsearchFlow {

  /**
   * Create a flow to update Elasticsearch with [[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`.
   * The result status is part of the [[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]] and must be checked for
   * successful execution.
   *
   * Warning: When settings configure retrying, messages are emitted out-of-order when errors are detected.
   *
   * @param objectMapper Jackson object mapper converting type `T` to JSON
   */
  def create[T](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      objectMapper: ObjectMapper)
      : pekko.stream.javadsl.Flow[WriteMessage[T, NotUsed], WriteResult[T, NotUsed], NotUsed] =
    create(elasticsearchParams, settings, new JacksonWriter[T](objectMapper))

  /**
   * Create a flow to update Elasticsearch with [[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`.
   * The result status is part of the [[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]] and must be checked for
   * successful execution.
   *
   * Warning: When settings configure retrying, messages are emitted out-of-order when errors are detected.
   *
   * @param messageWriter converts type `T` to a `String` containing valid JSON
   */
  def create[T](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      messageWriter: MessageWriter[T])
      : pekko.stream.javadsl.Flow[WriteMessage[T, NotUsed], WriteResult[T, NotUsed], NotUsed] =
    scaladsl.ElasticsearchFlow
      .create(elasticsearchParams, settings, messageWriter)
      .asJava

  /**
   * Create a flow to update Elasticsearch with [[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`
   * with `passThrough` of type `C`.
   * The result status is part of the [[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]] and must be checked for
   * successful execution.
   *
   * Warning: When settings configure retrying, messages are emitted out-of-order when errors are detected.
   *
   * @param objectMapper Jackson object mapper converting type `T` to JSON
   */
  def createWithPassThrough[T, C](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      objectMapper: ObjectMapper): pekko.stream.javadsl.Flow[WriteMessage[T, C], WriteResult[T, C], NotUsed] =
    createWithPassThrough(elasticsearchParams, settings, new JacksonWriter[T](objectMapper))

  /**
   * Create a flow to update Elasticsearch with [[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`
   * with `passThrough` of type `C`.
   * The result status is part of the [[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]] and must be checked for
   * successful execution.
   *
   * Warning: When settings configure retrying, messages are emitted out-of-order when errors are detected.
   *
   * @param messageWriter converts type `T` to a `String` containing valid JSON
   */
  def createWithPassThrough[T, C](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      messageWriter: MessageWriter[T]): pekko.stream.javadsl.Flow[WriteMessage[T, C], WriteResult[T, C], NotUsed] =
    scaladsl.ElasticsearchFlow
      .createWithPassThrough(elasticsearchParams, settings, messageWriter)
      .asJava

  /**
   * Create a flow to update Elasticsearch with
   * [[java.util.List[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`
   * with `passThrough` of type `C`.
   * The result status is part of the [[java.util.List[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]]]
   * and must be checked for successful execution.
   *
   * Warning: When settings configure retrying, messages are emitted out-of-order when errors are detected.
   *
   * @param objectMapper Jackson object mapper converting type `T` to JSON
   */
  def createBulk[T, C](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      objectMapper: ObjectMapper): pekko.stream.javadsl.Flow[java.util.List[WriteMessage[T, C]],
    java.util.List[WriteResult[T, C]], NotUsed] =
    createBulk(elasticsearchParams, settings, new JacksonWriter[T](objectMapper))

  /**
   * Create a flow to update Elasticsearch with
   * [[java.util.List[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`
   * with `passThrough` of type `C`.
   * The result status is part of the [[java.util.List[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]]]
   * and must be checked for successful execution.
   *
   * Warning: When settings configure retrying, messages are emitted out-of-order when errors are detected.
   *
   * @param messageWriter converts type `T` to a `String` containing valid JSON
   */
  def createBulk[T, C](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      messageWriter: MessageWriter[T]): pekko.stream.javadsl.Flow[java.util.List[WriteMessage[T, C]],
    java.util.List[WriteResult[T, C]], NotUsed] = pekko.stream.scaladsl
    .Flow[java.util.List[WriteMessage[T, C]]]
    .map(_.asScala.toIndexedSeq)
    .via(
      scaladsl.ElasticsearchFlow
        .createBulk(elasticsearchParams, settings, messageWriter))
    .map(_.asJava)
    .asJava

  /**
   * Create a flow to update Elasticsearch with [[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`
   * with `context` of type `C`.
   * The result status is part of the [[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]] and must be checked for
   * successful execution.
   *
   * @param objectMapper Jackson object mapper converting type `T` to JSON
   * @throws IllegalArgumentException When settings configure retrying.
   */
  @ApiMayChange
  def createWithContext[T, C](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      objectMapper: ObjectMapper)
      : pekko.stream.javadsl.FlowWithContext[WriteMessage[T, NotUsed], C, WriteResult[T, C], C, NotUsed] =
    createWithContext(elasticsearchParams, settings, new JacksonWriter[T](objectMapper))

  /**
   * Create a flow to update Elasticsearch with [[pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`
   * with `context` of type `C`.
   * The result status is part of the [[pekko.stream.connectors.elasticsearch.WriteResult WriteResult]] and must be checked for
   * successful execution.
   *
   * @param messageWriter converts type `T` to a `String` containing valid JSON
   * @throws IllegalArgumentException When settings configure retrying.
   */
  @ApiMayChange
  def createWithContext[T, C](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      messageWriter: MessageWriter[T])
      : pekko.stream.javadsl.FlowWithContext[WriteMessage[T, NotUsed], C, WriteResult[T, C], C, NotUsed] =
    scaladsl.ElasticsearchFlow
      .createWithContext(elasticsearchParams, settings, messageWriter)
      .asJava

  private final class JacksonWriter[T](mapper: ObjectMapper) extends MessageWriter[T] {

    override def convert(message: T): String =
      mapper.writeValueAsString(message)
  }

}
