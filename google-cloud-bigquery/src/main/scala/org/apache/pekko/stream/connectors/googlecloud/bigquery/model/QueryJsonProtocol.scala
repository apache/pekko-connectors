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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.model

import org.apache.pekko
import pekko.stream.connectors.google.scaladsl.Paginated
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray.BigQueryRestJsonProtocol._
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray.BigQueryRootJsonReader
import pekko.util.ccompat.JavaConverters._
import pekko.util.JavaDurationConverters._
import pekko.util.OptionConverters._
import com.fasterxml.jackson.annotation.{ JsonCreator, JsonIgnoreProperties, JsonProperty }
import spray.json.{ JsonFormat, RootJsonFormat, RootJsonReader }

import java.time.Duration
import java.util.{ Optional, OptionalInt, OptionalLong }
import java.{ lang, util }
import scala.annotation.nowarn
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

/**
 * QueryRequest model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query#queryrequest BigQuery reference]]
 *
 * @param query a query string, following the BigQuery query syntax, of the query to execute
 * @param maxResults the maximum number of rows of data to return per page of results
 * @param defaultDataset specifies the default datasetId and projectId to assume for any unqualified table names in the query
 * @param timeout specifies the maximum amount of time that the client is willing to wait for the query to complete
 * @param dryRun if set to `true` BigQuery doesn't run the job and instead returns statistics about the job such as how many bytes would be processed
 * @param useLegacySql specifies whether to use BigQuery's legacy SQL dialect for this query
 * @param location the geographic location where the job should run
 * @param labels the labels associated with this query
 * @param maximumBytesBilled limits the number of bytes billed for this query
 * @param requestId a unique user provided identifier to ensure idempotent behavior for queries
 */
final case class QueryRequest private[bigquery] (query: String,
    maxResults: Option[Int],
    defaultDataset: Option[DatasetReference],
    timeout: Option[FiniteDuration],
    dryRun: Option[Boolean],
    useLegacySql: Option[Boolean],
    location: Option[String],
    labels: Option[Map[String, String]],
    maximumBytesBilled: Option[Long],
    requestId: Option[String]) {

  def getQuery: String = query
  def getMaxResults: OptionalInt = maxResults.toJavaPrimitive
  def getDefaultDataset: Optional[DatasetReference] = defaultDataset.toJava
  def getTimeout: Optional[Duration] = timeout.map(_.asJava).toJava
  def getDryRun: Optional[lang.Boolean] = dryRun.map(lang.Boolean.valueOf).toJava
  def getUseLegacySql: Optional[lang.Boolean] = useLegacySql.map(lang.Boolean.valueOf).toJava
  def getRequestId: Optional[String] = requestId.toJava
  def getLocation: Optional[String] = location.toJava
  def getMaximumBytesBilled: Optional[Long] = maximumBytesBilled.toJava
  def getLabels: Optional[Map[String, String]] = labels.toJava

  def withQuery(query: String): QueryRequest =
    copy(query = query)

  def withMaxResults(maxResults: Option[Int]): QueryRequest =
    copy(maxResults = maxResults)
  def withMaxResults(maxResults: util.OptionalInt): QueryRequest =
    copy(maxResults = maxResults.toScala)

  def withDefaultDataset(defaultDataset: Option[DatasetReference]): QueryRequest =
    copy(defaultDataset = defaultDataset)
  def withDefaultDataset(defaultDataset: util.Optional[DatasetReference]): QueryRequest =
    copy(defaultDataset = defaultDataset.toScala)

  def withTimeout(timeout: Option[FiniteDuration]): QueryRequest =
    copy(timeout = timeout)
  def withTimeout(timeout: util.Optional[Duration]): QueryRequest =
    copy(timeout = timeout.toScala.map(_.asScala))

  def withDryRun(dryRun: Option[Boolean]): QueryRequest =
    copy(dryRun = dryRun)
  def withDryRun(dryRun: util.Optional[lang.Boolean]): QueryRequest =
    copy(dryRun = dryRun.toScala.map(_.booleanValue))

  def withUseLegacySql(useLegacySql: Option[Boolean]): QueryRequest =
    copy(useLegacySql = useLegacySql)
  def withUseLegacySql(useLegacySql: util.Optional[lang.Boolean]): QueryRequest =
    copy(useLegacySql = useLegacySql.toScala.map(_.booleanValue))

  def withRequestId(requestId: Option[String]): QueryRequest =
    copy(requestId = requestId)
  def withRequestId(requestId: util.Optional[String]): QueryRequest =
    copy(requestId = requestId.toScala)

  def withLocation(location: Option[String]): QueryRequest =
    copy(location = location)
  def withLocation(location: util.Optional[String]): QueryRequest =
    copy(location = location.toScala)

  def withMaximumBytesBilled(maximumBytesBilled: Option[Long]): QueryRequest =
    copy(maximumBytesBilled = maximumBytesBilled)
  def withMaximumBytesBilled(maximumBytesBilled: util.OptionalLong): QueryRequest =
    copy(maximumBytesBilled = maximumBytesBilled.toScala)

  def withLabels(labels: Option[Map[String, String]]): QueryRequest =
    copy(labels = labels)
  def withLabels(labels: util.Optional[util.Map[String, String]]): QueryRequest =
    copy(labels = labels.toScala.map(_.asScala.toMap))
}

object QueryRequest {

  def apply(query: String,
      maxResults: Option[Int],
      defaultDataset: Option[DatasetReference],
      timeout: Option[FiniteDuration],
      dryRun: Option[Boolean],
      useLegacySql: Option[Boolean],
      requestId: Option[String]): QueryRequest =
    QueryRequest(query, maxResults, defaultDataset, timeout, dryRun, useLegacySql, None, None, None, requestId)

  /**
   * Java API: QueryRequest model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query#queryrequest BigQuery reference]]
   *
   * @param query a query string, following the BigQuery query syntax, of the query to execute
   * @param maxResults the maximum number of rows of data to return per page of results
   * @param defaultDataset specifies the default datasetId and projectId to assume for any unqualified table names in the query
   * @param timeout specifies the maximum amount of time, in milliseconds, that the client is willing to wait for the query to complete
   * @param dryRun if set to `true` BigQuery doesn't run the job and instead returns statistics about the job such as how many bytes would be processed
   * @param useLegacySql specifies whether to use BigQuery's legacy SQL dialect for this query
   * @param requestId a unique user provided identifier to ensure idempotent behavior for queries
   * @return a [[QueryRequest]]
   */
  def create(query: String,
      maxResults: util.OptionalInt,
      defaultDataset: util.Optional[DatasetReference],
      timeout: util.Optional[Duration],
      dryRun: util.Optional[lang.Boolean],
      useLegacySql: util.Optional[lang.Boolean],
      requestId: util.Optional[String]): QueryRequest =
    QueryRequest(
      query,
      maxResults.toScala,
      defaultDataset.toScala,
      timeout.toScala.map(_.asScala),
      dryRun.toScala.map(_.booleanValue),
      useLegacySql.toScala.map(_.booleanValue),
      None,
      None,
      None,
      requestId.toScala)

  implicit val format: RootJsonFormat[QueryRequest] = jsonFormat(
    apply,
    "query",
    "maxResults",
    "defaultDataset",
    "timeoutMs",
    "dryRun",
    "useLegacySql",
    "location",
    "labels",
    "maximumBytesBilled",
    "requestId")
}

/**
 * QueryResponse model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query#response-body BigQuery reference]]
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/getQueryResults#response-body BigQuery reference]]
 *
 * @param schema the schema of the results
 * @param jobReference reference to the Job that was created to run the query
 * @param totalRows the total number of rows in the complete query result set, which can be more than the number of rows in this single page of results
 * @param pageToken a token used for paging results
 * @param rows an object with as many results as can be contained within the maximum permitted reply size
 * @param totalBytesProcessed the total number of bytes processed for this query
 * @param jobComplete whether the query has completed or not
 * @param errors the first errors or warnings encountered during the running of the job
 * @param cacheHit whether the query result was fetched from the query cache
 * @param numDmlAffectedRows the number of rows affected by a DML statement
 * @tparam T the data model for each row
 */
@JsonIgnoreProperties(ignoreUnknown = true)
final case class QueryResponse[+T] private[bigquery] (schema: Option[TableSchema],
    jobReference: JobReference,
    totalRows: Option[Long],
    pageToken: Option[String],
    rows: Option[Seq[T]],
    totalBytesProcessed: Option[Long],
    jobComplete: Boolean,
    errors: Option[Seq[ErrorProto]],
    cacheHit: Option[Boolean],
    numDmlAffectedRows: Option[Long]) {

  @nowarn("msg=never used")
  @JsonCreator
  private def this(@JsonProperty("schema") schema: TableSchema,
      @JsonProperty(value = "jobReference", required = true) jobReference: JobReference,
      @JsonProperty("totalRows") totalRows: String,
      @JsonProperty("pageToken") pageToken: String,
      @JsonProperty("rows") rows: util.List[T],
      @JsonProperty("totalBytesProcessed") totalBytesProcessed: String,
      @JsonProperty(value = "jobComplete", required = true) jobComplete: Boolean,
      @JsonProperty("errors") errors: util.List[ErrorProto],
      @JsonProperty("cacheHit") cacheHit: lang.Boolean,
      @JsonProperty("numDmlAffectedRows") numDmlAffectedRows: String) =
    this(
      Option(schema),
      jobReference,
      Option(totalRows).map(_.toLong),
      Option(pageToken),
      Option(rows).map(_.asScala.toList),
      Option(totalBytesProcessed).map(_.toLong),
      jobComplete,
      Option(errors).map(_.asScala.toList),
      Option(cacheHit).map(_.booleanValue),
      Option(numDmlAffectedRows).map(_.toLong))

  def getSchema: Optional[TableSchema] = schema.toJava
  def getJobReference: JobReference = jobReference
  def getTotalRows: OptionalLong = totalRows.toJavaPrimitive
  def getPageToken: Optional[String] = pageToken.toJava
  def getRows: util.Optional[util.List[T] @uncheckedVariance] = rows.map(_.asJava).toJava
  def getTotalBytesProcessed: OptionalLong = totalBytesProcessed.toJavaPrimitive
  def getJobComplete: Boolean = jobComplete
  def getErrors: Optional[util.List[ErrorProto]] = errors.map(_.asJava).toJava
  def getCacheHit: Optional[lang.Boolean] = cacheHit.map(lang.Boolean.valueOf).toJava
  def getNumDmlAffectedRows: OptionalLong = numDmlAffectedRows.toJavaPrimitive

  def withSchema(schema: Option[TableSchema]): QueryResponse[T] =
    copy(schema = schema)
  def withSchema(schema: util.Optional[TableSchema]): QueryResponse[T] =
    copy(schema = schema.toScala)

  def withJobReference(jobReference: JobReference): QueryResponse[T] =
    copy(jobReference = jobReference)

  def withTotalRows(totalRows: Option[Long]): QueryResponse[T] =
    copy(totalRows = totalRows)
  def withTotalRows(totalRows: util.OptionalLong): QueryResponse[T] =
    copy(totalRows = totalRows.toScala)

  def withPageToken(pageToken: Option[String]): QueryResponse[T] =
    copy(pageToken = pageToken)
  def withPageToken(pageToken: util.Optional[String]): QueryResponse[T] =
    copy(pageToken = pageToken.toScala)

  def withRows[S >: T](rows: Option[Seq[S]]): QueryResponse[S] =
    copy(rows = rows)
  def withRows(rows: util.Optional[util.List[T] @uncheckedVariance]): QueryResponse[T] =
    copy(rows = rows.toScala.map(_.asScala.toList))

  def withTotalBytesProcessed(totalBytesProcessed: Option[Long]): QueryResponse[T] =
    copy(totalBytesProcessed = totalBytesProcessed)
  def withTotalBytesProcessed(totalBytesProcessed: util.OptionalLong): QueryResponse[T] =
    copy(totalBytesProcessed = totalBytesProcessed.toScala)

  def withJobComplete(jobComplete: Boolean): QueryResponse[T] =
    copy(jobComplete = jobComplete)

  def withErrors(errors: Option[Seq[ErrorProto]]): QueryResponse[T] =
    copy(errors = errors)
  def withErrors(errors: util.Optional[util.List[ErrorProto]]): QueryResponse[T] =
    copy(errors = errors.toScala.map(_.asScala.toList))

  def withCacheHit(cacheHit: Option[Boolean]): QueryResponse[T] =
    copy(cacheHit = cacheHit)
  def withCacheHit(cacheHit: util.Optional[lang.Boolean]): QueryResponse[T] =
    copy(cacheHit = cacheHit.toScala.map(_.booleanValue))

  def withNumDmlAffectedRows(numDmlAffectedRows: Option[Long]): QueryResponse[T] =
    copy(numDmlAffectedRows = numDmlAffectedRows)
  def withNumDmlAffectedRows(numDmlAffectedRows: util.OptionalLong): QueryResponse[T] =
    copy(numDmlAffectedRows = numDmlAffectedRows.toScala)
}

object QueryResponse {

  /**
   * Java API: QueryResponse model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query#response-body BigQuery reference]]
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/getQueryResults#response-body BigQuery reference]]
   *
   * @param schema the schema of the results
   * @param jobReference reference to the Job that was created to run the query
   * @param totalRows the total number of rows in the complete query result set, which can be more than the number of rows in this single page of results
   * @param pageToken a token used for paging results
   * @param rows an object with as many results as can be contained within the maximum permitted reply size
   * @param totalBytesProcessed the total number of bytes processed for this query
   * @param jobComplete whether the query has completed or not
   * @param errors the first errors or warnings encountered during the running of the job
   * @param cacheHit whether the query result was fetched from the query cache
   * @param numDmlAffectedRows the number of rows affected by a DML statement
   * @tparam T the data model for each row
   * @return a [[QueryResponse]]
   */
  def create[T](schema: util.Optional[TableSchema],
      jobReference: JobReference,
      totalRows: util.OptionalLong,
      pageToken: util.Optional[String],
      rows: util.Optional[util.List[T]],
      totalBytesProcessed: util.OptionalLong,
      jobComplete: Boolean,
      errors: util.Optional[util.List[ErrorProto]],
      cacheHit: util.Optional[lang.Boolean],
      numDmlAffectedRows: util.OptionalLong): QueryResponse[T] =
    QueryResponse[T](
      schema.toScala,
      jobReference,
      totalRows.toScala,
      pageToken.toScala,
      rows.toScala.map(_.asScala.toList),
      totalBytesProcessed.toScala,
      jobComplete,
      errors.toScala.map(_.asScala.toList),
      cacheHit.toScala.map(_.booleanValue),
      numDmlAffectedRows.toScala)

  implicit def reader[T <: AnyRef](
      implicit reader: BigQueryRootJsonReader[T]): RootJsonReader[QueryResponse[T]] = {
    implicit val format: JsonFormat[T] = lift(reader)
    jsonFormat10(QueryResponse[T])
  }
  implicit val paginated: Paginated[QueryResponse[Any]] = _.pageToken
}
