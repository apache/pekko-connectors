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
import pekko.util.ccompat.JavaConverters._
import pekko.util.OptionConverters._
import com.fasterxml.jackson.annotation.{ JsonCreator, JsonProperty }
import spray.json.{ JsonFormat, RootJsonFormat }

import java.util
import java.util.{ Optional, OptionalInt, OptionalLong }
import scala.annotation.nowarn
import scala.annotation.varargs
import scala.collection.immutable.Seq

/**
 * Table resource model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#resource:-table BigQuery reference]]
 *
 * @param tableReference reference describing the ID of this table
 * @param labels the labels associated with this table
 * @param schema describes the schema of this table
 * @param numRows the number of rows of data in this table
 * @param location the geographic location where the table resides
 */
final case class Table private[bigquery] (tableReference: TableReference,
    labels: Option[Map[String, String]],
    schema: Option[TableSchema],
    numRows: Option[Long],
    location: Option[String]) {

  def getTableReference: TableReference = tableReference
  def getLabels: Optional[util.Map[String, String]] = labels.map(_.asJava).toJava
  def getSchema: Optional[TableSchema] = schema.toJava
  def getNumRows: OptionalLong = numRows.toJavaPrimitive
  def getLocation: Optional[String] = location.toJava

  def withTableReference(tableReference: TableReference): Table =
    copy(tableReference = tableReference)

  def withLabels(labels: Option[Map[String, String]]): Table =
    copy(labels = labels)
  def withLabels(labels: util.Optional[util.Map[String, String]]): Table =
    copy(labels = labels.toScala.map(_.asScala.toMap))

  def withSchema(schema: Option[TableSchema]): Table =
    copy(schema = schema)
  def withSchema(schema: util.Optional[TableSchema]): Table =
    copy(schema = schema.toScala)

  def withNumRows(numRows: Option[Long]): Table =
    copy(numRows = numRows)
  def withNumRows(numRows: util.OptionalLong): Table =
    copy(numRows = numRows.toScala)

  def withLocation(location: Option[String]): Table =
    copy(location = location)
  def withLocation(location: util.Optional[String]): Table =
    copy(location = location.toScala)
}

object Table {

  /**
   * Java API: Table resource model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#resource:-table BigQueryReference]]
   *
   * @param tableReference reference describing the ID of this table
   * @param labels the labels associated with this table
   * @param schema describes the schema of this table
   * @param numRows the number of rows of data in this table
   * @param location the geographic location where the table resides
   * @return a [[Table]]
   */
  def create(tableReference: TableReference,
      labels: util.Optional[util.Map[String, String]],
      schema: util.Optional[TableSchema],
      numRows: util.OptionalLong,
      location: util.Optional[String]): Table =
    Table(
      tableReference,
      labels.toScala.map(_.asScala.toMap),
      schema.toScala,
      numRows.toScala,
      location.toScala)

  implicit val format: RootJsonFormat[Table] = jsonFormat5(apply)
}

/**
 * TableReference model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/TableReference BigQuery reference]]
 *
 * @param projectId the ID of the project containing this table
 * @param datasetId the ID of the dataset containing this table
 * @param tableId the ID of the table
 */
final case class TableReference private[bigquery] (projectId: Option[String], datasetId: String,
    tableId: Option[String]) {

  def getProjectId: Optional[String] = projectId.toJava
  def getDatasetId: String = datasetId
  def getTableId: Option[String] = tableId

  def withProjectId(projectId: Option[String]): TableReference =
    copy(projectId = projectId)
  def withProjectId(projectId: util.Optional[String]): TableReference =
    copy(projectId = projectId.toScala)

  def withDatasetId(datasetId: String): TableReference =
    copy(datasetId = datasetId)

  def withTableId(tableId: Option[String]): TableReference =
    copy(tableId = tableId)
  def withTableId(tableId: util.Optional[String]): TableReference =
    copy(tableId = tableId.toScala)
}

object TableReference {

  /**
   * Java API: TableReference model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/TableReference BigQuery reference]]
   *
   * @param projectId the ID of the project containing this table
   * @param datasetId the ID of the dataset containing this table
   * @param tableId the ID of the table
   * @return a [[TableReference]]
   */
  def create(projectId: util.Optional[String], datasetId: String, tableId: util.Optional[String]): TableReference =
    TableReference(projectId.toScala, datasetId, tableId.toScala)

  implicit val referenceFormat: JsonFormat[TableReference] = jsonFormat3(apply)
}

/**
 * Schema of a table
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#tableschema BigQuery reference]]
 *
 * @param fields describes the fields in a table
 */
final case class TableSchema private[bigquery] (fields: Seq[TableFieldSchema]) {

  @nowarn("msg=never used")
  @JsonCreator
  private def this(@JsonProperty(value = "fields", required = true) fields: util.List[TableFieldSchema]) =
    this(fields.asScala.toList)

  def getFields: util.List[TableFieldSchema] = fields.asJava

  def withFields(fields: Seq[TableFieldSchema]): TableSchema =
    copy(fields = fields)
  def withFields(fields: util.List[TableFieldSchema]): TableSchema =
    copy(fields = fields.asScala.toList)
}

object TableSchema {

  /**
   * Java API: Schema of a table
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#tableschema BigQuery reference]]
   *
   * @param fields describes the fields in a table
   * @return a [[TableSchema]]
   */
  def create(fields: util.List[TableFieldSchema]): TableSchema = TableSchema(fields.asScala.toList)

  /**
   * Java API: Schema of a table
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#tableschema BigQuery reference]]
   *
   * @param fields describes the fields in a table
   * @return a [[TableSchema]]
   */
  @varargs
  def create(fields: TableFieldSchema*): TableSchema = TableSchema(fields.toList)

  implicit val format: JsonFormat[TableSchema] = jsonFormat1(apply)
}

/**
 * A field in TableSchema
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#tablefieldschema BigQuery reference]]
 *
 * @param name the field name
 * @param `type` the field data type
 * @param mode the field mode
 * @param fields describes the nested schema fields if the type property is set to `RECORD`
 */
final case class TableFieldSchema private[bigquery] (name: String,
    `type`: TableFieldSchemaType,
    mode: Option[TableFieldSchemaMode],
    fields: Option[Seq[TableFieldSchema]]) {

  @nowarn("msg=never used")
  @JsonCreator
  private def this(@JsonProperty(value = "name", required = true) name: String,
      @JsonProperty(value = "type", required = true) `type`: String,
      @JsonProperty("mode") mode: String,
      @JsonProperty("fields") fields: util.List[TableFieldSchema]) =
    this(
      name,
      TableFieldSchemaType(`type`),
      Option(mode).map(TableFieldSchemaMode.apply),
      Option(fields).map(_.asScala.toList))

  def getName: String = name
  def getType: TableFieldSchemaType = `type`
  def getMode: Optional[TableFieldSchemaMode] = mode.toJava
  def getFields: Optional[util.List[TableFieldSchema]] = fields.map(_.asJava).toJava

  def withName(name: String): TableFieldSchema =
    copy(name = name)

  def withType(`type`: TableFieldSchemaType): TableFieldSchema =
    copy(`type` = `type`)

  def withMode(mode: Option[TableFieldSchemaMode]): TableFieldSchema =
    copy(mode = mode)
  def withMode(mode: util.Optional[TableFieldSchemaMode]): TableFieldSchema =
    copy(mode = mode.toScala)

  def withFields(fields: Option[Seq[TableFieldSchema]]): TableFieldSchema =
    copy(fields = fields)
  def withFields(fields: util.Optional[util.List[TableFieldSchema]]): TableFieldSchema =
    copy(fields = fields.toScala.map(_.asScala.toList))
}

object TableFieldSchema {

  /**
   * A field in TableSchema
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#tablefieldschema BigQuery reference]]
   *
   * @param name the field name
   * @param `type` the field data type
   * @param mode the field mode
   * @param fields describes the nested schema fields if the type property is set to `RECORD`
   * @return a [[TableFieldSchema]]
   */
  def create(name: String,
      `type`: TableFieldSchemaType,
      mode: util.Optional[TableFieldSchemaMode],
      fields: util.Optional[util.List[TableFieldSchema]]): TableFieldSchema =
    TableFieldSchema(name, `type`, mode.toScala, fields.toScala.map(_.asScala.toList))

  /**
   * A field in TableSchema
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables#tablefieldschema BigQuery reference]]
   *
   * @param name the field name
   * @param `type` the field data type
   * @param mode the field mode
   * @param fields describes the nested schema fields if the type property is set to `RECORD`
   * @return a [[TableFieldSchema]]
   */
  @varargs
  def create(name: String,
      `type`: TableFieldSchemaType,
      mode: util.Optional[TableFieldSchemaMode],
      fields: TableFieldSchema*): TableFieldSchema =
    TableFieldSchema(name, `type`, mode.toScala, if (fields.nonEmpty) Some(fields.toList) else None)

  implicit val format: JsonFormat[TableFieldSchema] = lazyFormat(
    jsonFormat(apply, "name", "type", "mode", "fields"))
}

final case class TableFieldSchemaType private[bigquery] (value: String) extends StringEnum
object TableFieldSchemaType {

  /**
   * Java API
   */
  def create(value: String): TableFieldSchemaType = TableFieldSchemaType(value)

  val String: TableFieldSchemaType = TableFieldSchemaType("STRING")
  def string: TableFieldSchemaType = String

  val Bytes: TableFieldSchemaType = TableFieldSchemaType("BYTES")
  def bytes: TableFieldSchemaType = Bytes

  val Integer: TableFieldSchemaType = TableFieldSchemaType("INTEGER")
  def integer: TableFieldSchemaType = Integer

  val Float: TableFieldSchemaType = TableFieldSchemaType("FLOAT")
  def float64: TableFieldSchemaType = Float // float is a reserved keyword in Java

  val Boolean: TableFieldSchemaType = TableFieldSchemaType("BOOLEAN")
  def bool: TableFieldSchemaType = Boolean // boolean is a reserved keyword in Java

  val Timestamp: TableFieldSchemaType = TableFieldSchemaType("TIMESTAMP")
  def timestamp: TableFieldSchemaType = Timestamp

  val Date: TableFieldSchemaType = TableFieldSchemaType("DATE")
  def date: TableFieldSchemaType = Date

  val Time: TableFieldSchemaType = TableFieldSchemaType("TIME")
  def time: TableFieldSchemaType = Time

  val DateTime: TableFieldSchemaType = TableFieldSchemaType("DATETIME")
  def dateTime: TableFieldSchemaType = DateTime

  val Geography: TableFieldSchemaType = TableFieldSchemaType("GEOGRAPHY")
  def geography: TableFieldSchemaType = Geography

  val Numeric: TableFieldSchemaType = TableFieldSchemaType("NUMERIC")
  def numeric: TableFieldSchemaType = Numeric

  val BigNumeric: TableFieldSchemaType = TableFieldSchemaType("BIGNUMERIC")
  def bigNumeric: TableFieldSchemaType = BigNumeric

  val Record: TableFieldSchemaType = TableFieldSchemaType("RECORD")
  def record: TableFieldSchemaType = Record

  implicit val format: JsonFormat[TableFieldSchemaType] = StringEnum.jsonFormat(apply)
}

final case class TableFieldSchemaMode private[bigquery] (value: String) extends StringEnum
object TableFieldSchemaMode {

  /**
   * Java API
   */
  def create(value: String): TableFieldSchemaMode = TableFieldSchemaMode(value)

  val Nullable: TableFieldSchemaMode = TableFieldSchemaMode("NULLABLE")
  def nullable: TableFieldSchemaMode = Nullable

  val Required: TableFieldSchemaMode = TableFieldSchemaMode("REQUIRED")
  def required: TableFieldSchemaMode = Required

  val Repeated: TableFieldSchemaMode = TableFieldSchemaMode("REPEATED")
  def repeated: TableFieldSchemaMode = Repeated

  implicit val format: JsonFormat[TableFieldSchemaMode] = StringEnum.jsonFormat(apply)
}

/**
 * TableListResponse model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/list#response-body BigQuery reference]]
 *
 * @param nextPageToken a token to request the next page of results
 * @param tables tables in the requested dataset
 * @param totalItems the total number of tables in the dataset
 */
final case class TableListResponse private[bigquery] (nextPageToken: Option[String],
    tables: Option[Seq[Table]],
    totalItems: Option[Int]) {

  def getNextPageToken: Optional[String] = nextPageToken.toJava
  def getTables: Optional[util.List[Table]] = tables.map(_.asJava).toJava
  def getTotalItems: OptionalInt = totalItems.toJavaPrimitive

  def withNextPageToken(nextPageToken: util.Optional[String]): TableListResponse =
    copy(nextPageToken = nextPageToken.toScala)
  def withTables(tables: util.Optional[util.List[Table]]): TableListResponse =
    copy(tables = tables.toScala.map(_.asScala.toList))
  def withTotalItems(totalItems: util.OptionalInt): TableListResponse =
    copy(totalItems = totalItems.toScala)
}

object TableListResponse {

  /**
   * Java API: TableListResponse model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/tables/list#response-body BigQuery reference]]
   *
   * @param nextPageToken a token to request the next page of results
   * @param tables tables in the requested dataset
   * @param totalItems the total number of tables in the dataset
   * @return a [[TableListResponse]]
   */
  def createTableListResponse(nextPageToken: util.Optional[String],
      tables: util.Optional[util.List[Table]],
      totalItems: util.OptionalInt): TableListResponse =
    TableListResponse(nextPageToken.toScala, tables.toScala.map(_.asScala.toList), totalItems.toScala)

  implicit val format: RootJsonFormat[TableListResponse] = jsonFormat3(apply)
  implicit val paginated: Paginated[TableListResponse] = _.nextPageToken
}
