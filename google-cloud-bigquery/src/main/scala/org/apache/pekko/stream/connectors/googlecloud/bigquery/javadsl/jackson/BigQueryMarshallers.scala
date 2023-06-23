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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.javadsl.jackson

import org.apache.pekko
import pekko.http.javadsl.marshallers.jackson.Jackson
import pekko.http.javadsl.marshalling.Marshaller
import pekko.http.javadsl.model.{ HttpEntity, MediaTypes, RequestEntity }
import pekko.http.javadsl.unmarshalling.Unmarshaller
import pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse
import pekko.stream.connectors.googlecloud.bigquery.model.{ TableDataInsertAllRequest, TableDataListResponse }
import com.fasterxml.jackson.databind.{ JavaType, MapperFeature, ObjectMapper }

import java.io.IOException

object BigQueryMarshallers {

  private val defaultObjectMapper = new ObjectMapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)

  /**
   * [[pekko.http.javadsl.unmarshalling.Unmarshaller]] for [[pekko.stream.connectors.googlecloud.bigquery.model.TableDataListResponse]]
   *
   * @param `type` the data model for each row
   * @tparam T the data model for each row
   */
  def tableDataListResponseUnmarshaller[T](`type`: Class[T]): Unmarshaller[HttpEntity, TableDataListResponse[T]] =
    unmarshaller(defaultObjectMapper.getTypeFactory.constructParametricType(classOf[TableDataListResponse[T]], `type`))

  /**
   * [[pekko.http.javadsl.unmarshalling.Unmarshaller]] for [[pekko.stream.connectors.googlecloud.bigquery.model.TableDataListResponse]]
   *
   * @param mapper an [[ObjectMapper]]
   * @param `type` the data model for each row
   * @tparam T the data model for each row
   */
  def tableDataListResponseUnmarshaller[T](mapper: ObjectMapper,
      `type`: Class[T]): Unmarshaller[HttpEntity, TableDataListResponse[T]] =
    unmarshaller(mapper, mapper.getTypeFactory.constructParametricType(classOf[TableDataListResponse[T]], `type`))

  /**
   * [[pekko.http.javadsl.marshalling.Marshaller]] for [[pekko.stream.connectors.googlecloud.bigquery.model.TableDataInsertAllRequest]]
   *
   * @tparam T the data model for each row
   */
  def tableDataInsertAllRequestMarshaller[T](): Marshaller[TableDataInsertAllRequest[T], RequestEntity] =
    Jackson.marshaller[TableDataInsertAllRequest[T]]()

  /**
   * [[pekko.http.javadsl.marshalling.Marshaller]] for [[pekko.stream.connectors.googlecloud.bigquery.model.TableDataInsertAllRequest]]
   *
   * @param mapper an [[ObjectMapper]]
   * @tparam T the data model for each row
   */
  def tableDataInsertAllRequestMarshaller[T](
      mapper: ObjectMapper): Marshaller[TableDataInsertAllRequest[T], RequestEntity] =
    Jackson.marshaller[TableDataInsertAllRequest[T]](mapper)

  /**
   * [[pekko.http.javadsl.unmarshalling.Unmarshaller]] for [[pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse]]
   *
   * @param `type` the data model for each row
   * @tparam T the data model for each row
   */
  def queryResponseUnmarshaller[T](`type`: Class[T]): Unmarshaller[HttpEntity, QueryResponse[T]] =
    unmarshaller(defaultObjectMapper.getTypeFactory.constructParametricType(classOf[QueryResponse[T]], `type`))

  /**
   * [[pekko.http.javadsl.unmarshalling.Unmarshaller]] for [[pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse]]
   *
   * @param mapper an [[ObjectMapper]]
   * @param `type` the data model for each row
   * @tparam T the data model for each row
   */
  def queryResponseUnmarshaller[T](mapper: ObjectMapper, `type`: Class[T]): Unmarshaller[HttpEntity, QueryResponse[T]] =
    unmarshaller(mapper, mapper.getTypeFactory.constructParametricType(classOf[QueryResponse[T]], `type`))

  def unmarshaller[T](expectedType: JavaType): Unmarshaller[HttpEntity, T] =
    unmarshaller(defaultObjectMapper, expectedType)

  def unmarshaller[T](mapper: ObjectMapper, expectedType: JavaType): Unmarshaller[HttpEntity, T] =
    Unmarshaller
      .forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString)
      .thenApply(fromJson(mapper, _, expectedType))

  private def fromJson[T](mapper: ObjectMapper, json: String, expectedType: JavaType) =
    try mapper.readerFor(expectedType).readValue[T](json)
    catch {
      case e: IOException =>
        throw new IllegalArgumentException("Cannot unmarshal JSON as " + expectedType.getTypeName, e)
    }
}
