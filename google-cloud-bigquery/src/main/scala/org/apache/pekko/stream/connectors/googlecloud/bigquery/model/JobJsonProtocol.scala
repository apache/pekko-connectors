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
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray.BigQueryRestJsonProtocol._
import pekko.util.ccompat.JavaConverters._
import pekko.util.OptionConverters._
import com.fasterxml.jackson.annotation.{ JsonCreator, JsonProperty }
import spray.json.{ JsonFormat, RootJsonFormat }

import java.util
import java.util.Optional
import scala.annotation.nowarn
import scala.collection.immutable.Seq

/**
 * Job model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job BigQuery reference]]
 *
 * @param configuration describes the job configuration
 * @param jobReference reference describing the unique-per-user name of the job
 * @param status the status of this job
 */
final case class Job private[bigquery] (configuration: Option[JobConfiguration],
    jobReference: Option[JobReference],
    status: Option[JobStatus]) {

  def getConfiguration: Optional[JobConfiguration] = configuration.toJava
  def getJobReference: Optional[JobReference] = jobReference.toJava
  def getStatus: Optional[JobStatus] = status.toJava

  def withConfiguration(configuration: Option[JobConfiguration]): Job =
    copy(configuration = configuration)
  def withConfiguration(configuration: util.Optional[JobConfiguration]): Job =
    copy(configuration = configuration.toScala)

  def withJobReference(jobReference: Option[JobReference]): Job =
    copy(jobReference = jobReference)
  def withJobReference(jobReference: util.Optional[JobReference]): Job =
    copy(jobReference = jobReference.toScala)

  def withStatus(status: Option[JobStatus]): Job =
    copy(status = status)
  def withStatus(status: util.Optional[JobStatus]): Job =
    copy(status = status.toScala)
}

object Job {

  /**
   * Java API: Job model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job BigQuery reference]]
   *
   * @param configuration describes the job configuration
   * @param jobReference reference describing the unique-per-user name of the job
   * @param status the status of this job
   * @return a [[Job]]
   */
  def create(configuration: util.Optional[JobConfiguration],
      jobReference: util.Optional[JobReference],
      status: util.Optional[JobStatus]): Job =
    Job(configuration.toScala, jobReference.toScala, status.toScala)

  implicit val format: RootJsonFormat[Job] = jsonFormat3(apply)
}

/**
 * JobConfiguration model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfiguration BigQuery reference]]
 *
 * @param load configures a load job
 * @param labels the labels associated with this job
 */
final case class JobConfiguration private[bigquery] (load: Option[JobConfigurationLoad],
    labels: Option[Map[String, String]]) {
  def getLoad: Optional[JobConfigurationLoad] = load.toJava
  def getLabels: Optional[Map[String, String]] = labels.toJava

  def withLoad(load: Option[JobConfigurationLoad]): JobConfiguration =
    copy(load = load)
  def withLoad(load: util.Optional[JobConfigurationLoad]): JobConfiguration =
    copy(load = load.toScala)

  def withLabels(labels: Option[Map[String, String]]): JobConfiguration =
    copy(labels = labels)
  def withLabels(labels: util.Optional[util.Map[String, String]]): JobConfiguration =
    copy(labels = labels.toScala.map(_.asScala.toMap))
}

object JobConfiguration {

  /**
   * JobConfiguration model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfiguration BigQuery reference]]
   *
   * @param load configures a load job
   * @return a [[JobConfiguration]]
   */
  def apply(load: Option[JobConfigurationLoad]): JobConfiguration =
    apply(load, None)

  /**
   * Java API: JobConfiguration model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfiguration BigQuery reference]]
   *
   * @param load configures a load job
   * @return a [[JobConfiguration]]
   */
  def create(load: util.Optional[JobConfigurationLoad]): JobConfiguration =
    JobConfiguration(load.toScala)

  /**
   * Java API: JobConfiguration model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfiguration BigQuery reference]]
   *
   * @param load configures a load job
   * @param labels the labels associated with this job
   * @return a [[JobConfiguration]]
   */
  def create(
      load: util.Optional[JobConfigurationLoad], labels: util.Optional[util.Map[String, String]]): JobConfiguration =
    JobConfiguration(load.toScala, labels.toScala.map(_.asScala.toMap))

  implicit val format: JsonFormat[JobConfiguration] = jsonFormat2(apply)
}

/**
 * JobConfigurationLoad model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfigurationload BigQuery reference]]
 *
 * @param schema the schema for the destination table
 * @param destinationTable the destination table to load the data into
 * @param createDisposition specifies whether the job is allowed to create new tables
 * @param writeDisposition specifies the action that occurs if the destination table already exists
 * @param sourceFormat the format of the data files
 */
final case class JobConfigurationLoad private[bigquery] (schema: Option[TableSchema],
    destinationTable: Option[TableReference],
    createDisposition: Option[CreateDisposition],
    writeDisposition: Option[WriteDisposition],
    sourceFormat: Option[SourceFormat]) {

  def getSchema: Optional[TableSchema] = schema.toJava
  def getDestinationTable: Optional[TableReference] = destinationTable.toJava
  def getCreateDisposition: Optional[CreateDisposition] = createDisposition.toJava
  def getWriteDisposition: Optional[WriteDisposition] = writeDisposition.toJava
  def getSourceFormat: Optional[SourceFormat] = sourceFormat.toJava

  def withSchema(schema: Option[TableSchema]): JobConfigurationLoad =
    copy(schema = schema)
  def withSchema(schema: util.Optional[TableSchema]): JobConfigurationLoad =
    copy(schema = schema.toScala)

  def withDestinationTable(destinationTable: Option[TableReference]): JobConfigurationLoad =
    copy(destinationTable = destinationTable)
  def withDestinationTable(destinationTable: util.Optional[TableReference]): JobConfigurationLoad =
    copy(destinationTable = destinationTable.toScala)

  def withCreateDisposition(createDisposition: Option[CreateDisposition]): JobConfigurationLoad =
    copy(createDisposition = createDisposition)
  def withCreateDisposition(createDisposition: util.Optional[CreateDisposition]): JobConfigurationLoad =
    copy(createDisposition = createDisposition.toScala)

  def withWriteDisposition(writeDisposition: Option[WriteDisposition]): JobConfigurationLoad =
    copy(writeDisposition = writeDisposition)
  def withWriteDisposition(writeDisposition: util.Optional[WriteDisposition]): JobConfigurationLoad =
    copy(writeDisposition = writeDisposition.toScala)

  def withSourceFormat(sourceFormat: Option[SourceFormat]): JobConfigurationLoad =
    copy(sourceFormat = sourceFormat)
  def withSourceFormat(sourceFormat: util.Optional[SourceFormat]): JobConfigurationLoad =
    copy(sourceFormat = sourceFormat.toScala)
}

object JobConfigurationLoad {

  /**
   * Java API: JobConfigurationLoad model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfigurationload BigQuery reference]]
   *
   * @param schema the schema for the destination table
   * @param destinationTable the destination table to load the data into
   * @param createDisposition specifies whether the job is allowed to create new tables
   * @param writeDisposition specifies the action that occurs if the destination table already exists
   * @param sourceFormat the format of the data files
   * @return a [[JobConfigurationLoad]]
   */
  def create(schema: util.Optional[TableSchema],
      destinationTable: util.Optional[TableReference],
      createDisposition: util.Optional[CreateDisposition],
      writeDisposition: util.Optional[WriteDisposition],
      sourceFormat: util.Optional[SourceFormat]): JobConfigurationLoad =
    JobConfigurationLoad(
      schema.toScala,
      destinationTable.toScala,
      createDisposition.toScala,
      writeDisposition.toScala,
      sourceFormat.toScala)

  implicit val configurationLoadFormat: JsonFormat[JobConfigurationLoad] = jsonFormat5(apply)
}

final case class CreateDisposition private[bigquery] (value: String) extends StringEnum
object CreateDisposition {

  /**
   * Java API
   */
  def create(value: String): CreateDisposition = CreateDisposition(value)

  val CreateIfNeeded: CreateDisposition = CreateDisposition("CREATE_IF_NEEDED")
  def createIfNeeded: CreateDisposition = CreateIfNeeded

  val CreateNever: CreateDisposition = CreateDisposition("CREATE_NEVER")
  def createNever: CreateDisposition = CreateNever

  implicit val format: JsonFormat[CreateDisposition] = StringEnum.jsonFormat(apply)
}

final case class WriteDisposition private[bigquery] (value: String) extends StringEnum
object WriteDisposition {

  /**
   * Java API
   */
  def create(value: String): WriteDisposition = WriteDisposition(value)

  val WriteTruncate: WriteDisposition = WriteDisposition("WRITE_TRUNCATE")
  def writeTruncate: WriteDisposition = WriteTruncate

  val WriteAppend: WriteDisposition = WriteDisposition("WRITE_APPEND")
  def writeAppend: WriteDisposition = WriteAppend

  val WriteEmpty: WriteDisposition = WriteDisposition("WRITE_EMPTY")
  def writeEmpty: WriteDisposition = WriteEmpty

  implicit val format: JsonFormat[WriteDisposition] = StringEnum.jsonFormat(apply)
}

sealed case class SourceFormat private (value: String) extends StringEnum
object SourceFormat {

  /**
   * Java API
   */
  def create(value: String): SourceFormat = SourceFormat(value)

  val NewlineDelimitedJsonFormat: SourceFormat = SourceFormat("NEWLINE_DELIMITED_JSON")
  def newlineDelimitedJsonFormat: SourceFormat = NewlineDelimitedJsonFormat

  implicit val format: JsonFormat[SourceFormat] = StringEnum.jsonFormat(apply)
}

/**
 * JobReference model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/JobReference BigQuery reference]]
 *
 * @param projectId the ID of the project containing this job.
 * @param jobId the ID of the job
 * @param location the geographic location of the job
 */
final case class JobReference private[bigquery] (projectId: Option[String], jobId: Option[String],
    location: Option[String]) {

  @nowarn("msg=never used")
  @JsonCreator
  private def this(@JsonProperty("projectId") projectId: String,
      @JsonProperty("jobId") jobId: String,
      @JsonProperty("location") location: String) =
    this(Option(projectId), Option(jobId), Option(location))

  def getProjectId: Optional[String] = projectId.toJava
  def getJobId: Optional[String] = jobId.toJava
  def getLocation: Optional[String] = location.toJava

  def withProjectId(projectId: Option[String]): JobReference =
    copy(projectId = projectId)
  def withProjectId(projectId: util.Optional[String]): JobReference =
    copy(projectId = projectId.toScala)

  def withJobId(jobId: Option[String]): JobReference =
    copy(jobId = jobId)
  def withJobId(jobId: util.Optional[String]): JobReference =
    copy(jobId = jobId.toScala)

  def withLocation(location: Option[String]): JobReference =
    copy(location = location)
  def withLocation(location: util.Optional[String]): JobReference =
    copy(location = location.toScala)
}

object JobReference {

  /**
   * Java API: JobReference model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/JobReference BigQuery reference]]
   *
   * @param projectId the ID of the project containing this job.
   * @param jobId the ID of the job
   * @param location the geographic location of the job
   * @return a [[JobReference]]
   */
  def create(
      projectId: util.Optional[String], jobId: util.Optional[String], location: util.Optional[String]): JobReference =
    JobReference(projectId.toScala, jobId.toScala, location.toScala)

  implicit val format: JsonFormat[JobReference] = jsonFormat3(apply)
}

/**
 * JobStatus model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobstatus BigQuery reference]]
 *
 * @param errorResult final error result of the job; if present, indicates that the job has completed and was unsuccessful
 * @param errors the first errors encountered during the running of the job
 * @param state running state of the job
 */
final case class JobStatus private[bigquery] (errorResult: Option[ErrorProto], errors: Option[Seq[ErrorProto]],
    state: JobState) {

  def getErrorResult: Optional[ErrorProto] = errorResult.toJava
  def getErrors: Optional[util.List[ErrorProto]] = errors.map(_.asJava).toJava
  def getState: JobState = state

  def withErrorResult(errorResult: Option[ErrorProto]): JobStatus =
    copy(errorResult = errorResult)
  def withErrorResult(errorResult: util.Optional[ErrorProto]): JobStatus =
    copy(errorResult = errorResult.toScala)

  def withErrors(errors: Option[Seq[ErrorProto]]): JobStatus =
    copy(errors = errors)
  def withErrors(errors: util.Optional[util.List[ErrorProto]]): JobStatus =
    copy(errors = errors.toScala.map(_.asScala.toList))

  def withState(state: JobState): JobStatus =
    copy(state = state)
}

object JobStatus {

  /**
   * Java API: JobStatus model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobstatus BigQuery reference]]
   *
   * @param errorResult final error result of the job; if present, indicates that the job has completed and was unsuccessful
   * @param errors the first errors encountered during the running of the job
   * @param state running state of the job
   * @return a [[JobStatus]]
   */
  def create(errorResult: util.Optional[ErrorProto], errors: util.Optional[util.List[ErrorProto]], state: JobState)
      : JobStatus =
    JobStatus(errorResult.toScala, errors.toScala.map(_.asScala.toList), state)

  implicit val format: JsonFormat[JobStatus] = jsonFormat3(apply)
}

final case class JobState private[bigquery] (value: String) extends StringEnum
object JobState {

  /**
   * Java API
   */
  def create(value: String): JobState = JobState(value)

  val Pending: JobState = JobState("PENDING")
  def pending: JobState = Pending

  val Running: JobState = JobState("RUNNING")
  def running: JobState = Running

  val Done: JobState = JobState("DONE")
  def done: JobState = Done

  implicit val format: JsonFormat[JobState] = StringEnum.jsonFormat(apply)
}

final case class JobCancelResponse private[bigquery] (job: Job) {
  def getJob: Job = job
  def withJob(job: Job): JobCancelResponse =
    copy(job = job)
}

object JobCancelResponse {

  /**
   * Java API
   */
  def create(job: Job): JobCancelResponse = JobCancelResponse(job)

  implicit val format: RootJsonFormat[JobCancelResponse] = jsonFormat1(apply)
}
