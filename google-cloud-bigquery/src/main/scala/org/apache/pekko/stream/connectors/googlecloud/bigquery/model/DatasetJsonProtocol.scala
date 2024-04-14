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
import spray.json.{ JsonFormat, RootJsonFormat }

import java.util
import java.util.Optional
import scala.collection.immutable.Seq

/**
 * Dataset resource model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets#resource:-dataset BigQuery reference]]
 *
 * @param datasetReference a reference that identifies the dataset
 * @param friendlyName a descriptive name for the dataset
 * @param labels the labels associated with this dataset
 * @param location the geographic location where the dataset should reside
 */
final case class Dataset private[bigquery] (datasetReference: DatasetReference,
    friendlyName: Option[String],
    labels: Option[Map[String, String]],
    location: Option[String]) {

  def getDatasetReference: DatasetReference = datasetReference
  def getFriendlyName: Optional[String] = friendlyName.toJava
  def getLabels: Optional[util.Map[String, String]] = labels.map(_.asJava).toJava
  def getLocation: Optional[String] = location.toJava

  def withDatasetReference(datasetReference: DatasetReference): Dataset =
    copy(datasetReference = datasetReference)

  def withFriendlyName(friendlyName: Option[String]): Dataset =
    copy(friendlyName = friendlyName)
  def withFriendlyName(friendlyName: util.Optional[String]): Dataset =
    copy(friendlyName = friendlyName.toScala)

  def withLabels(labels: Option[Map[String, String]]): Dataset =
    copy(labels = labels)
  def withLabels(labels: util.Optional[util.Map[String, String]]): Dataset =
    copy(labels = labels.toScala.map(_.asScala.toMap))

  def withLocation(location: util.Optional[String]): Dataset =
    copy(location = location.toScala)
}

object Dataset {

  /**
   * Java API: Dataset resource model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets#resource:-dataset BigQuery reference]]
   *
   * @param datasetReference a reference that identifies the dataset
   * @param friendlyName a descriptive name for the dataset
   * @param labels the labels associated with this dataset
   * @param location the geographic location where the dataset should reside
   * @return a [[Dataset]]
   */
  def create(datasetReference: DatasetReference,
      friendlyName: util.Optional[String],
      labels: util.Optional[util.Map[String, String]],
      location: util.Optional[String]): Dataset =
    Dataset(datasetReference, friendlyName.toScala, labels.toScala.map(_.asScala.toMap), location.toScala)

  implicit val format: RootJsonFormat[Dataset] = jsonFormat4(apply)
}

/**
 * DatasetReference model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets#datasetreference BigQuery reference]]
 *
 * @param datasetId A unique ID for this dataset, without the project name
 * @param projectId The ID of the project containing this dataset
 */
final case class DatasetReference private[bigquery] (datasetId: Option[String], projectId: Option[String]) {

  def getDatasetId: Optional[String] = datasetId.toJava
  def getProjectId: Optional[String] = projectId.toJava

  def withDatasetId(datasetId: Option[String]): DatasetReference =
    copy(datasetId = datasetId)
  def withDatasetId(datasetId: util.Optional[String]): DatasetReference =
    copy(datasetId = datasetId.toScala)

  def withProjectId(projectId: Option[String]): DatasetReference =
    copy(projectId = projectId)
  def withProjectId(projectId: util.Optional[String]): DatasetReference =
    copy(projectId = projectId.toScala)
}

object DatasetReference {

  /**
   * Java API: DatasetReference model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets#datasetreference BigQuery reference]]
   *
   * @param datasetId A unique ID for this dataset, without the project name
   * @param projectId The ID of the project containing this dataset
   * @return a [[DatasetReference]]
   */
  def create(datasetId: util.Optional[String], projectId: util.Optional[String]): DatasetReference =
    DatasetReference(datasetId.toScala, projectId.toScala)

  implicit val format: JsonFormat[DatasetReference] = jsonFormat2(apply)
}

/**
 * DatasetListResponse model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/list#response-body BigQuery reference]]
 *
 * @param nextPageToken a token that can be used to request the next results page
 * @param datasets an array of the dataset resources in the project
 */
final case class DatasetListResponse private[bigquery] (nextPageToken: Option[String], datasets: Option[Seq[Dataset]]) {

  def getNextPageToken: Optional[String] = nextPageToken.toJava
  def getDatasets: Optional[util.List[Dataset]] = datasets.map(_.asJava).toJava

  def withNextPageToken(nextPageToken: Option[String]): DatasetListResponse =
    copy(nextPageToken = nextPageToken)
  def withNextPageToken(nextPageToken: util.Optional[String]): DatasetListResponse =
    copy(nextPageToken = nextPageToken.toScala)

  def withDatasets(datasets: Option[Seq[Dataset]]): DatasetListResponse =
    copy(datasets = datasets)
  def withDatasets(datasets: util.Optional[util.List[Dataset]]): DatasetListResponse =
    copy(datasets = datasets.toScala.map(_.asScala.toList))
}

object DatasetListResponse {

  /**
   * Java API: DatasetListResponse model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/datasets/list#response-body BigQuery reference]]
   *
   * @param nextPageToken a token that can be used to request the next results page
   * @param datasets an array of the dataset resources in the project
   * @return a [[DatasetListResponse]]
   */
  def create(nextPageToken: util.Optional[String], datasets: util.Optional[util.List[Dataset]]): DatasetListResponse =
    DatasetListResponse(nextPageToken.toScala, datasets.toScala.map(_.asScala.toList))

  implicit val format: RootJsonFormat[DatasetListResponse] = jsonFormat2(apply)
  implicit val paginated: Paginated[DatasetListResponse] = _.nextPageToken
}
