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

package docs.scaladsl

//#imports
import org.apache.pekko
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import pekko.stream.connectors.googlecloud.bigquery.InsertAllRetryPolicy
import pekko.stream.connectors.googlecloud.bigquery.model.{
  Dataset,
  Job,
  JobReference,
  JobState,
  QueryResponse,
  Table,
  TableDataListResponse,
  TableListResponse
}
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.schema.BigQuerySchemas._
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.schema.TableSchemaWriter
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray.BigQueryRootJsonFormat
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray.BigQueryJsonProtocol._
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.BigQuery
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import pekko.{ Done, NotUsed }

import scala.annotation.nowarn
import scala.collection.immutable.Seq
import scala.concurrent.Future
//#imports

class BigQueryDoc {

  @nowarn("msg=dead code")
  implicit val system: pekko.actor.ActorSystem = ???
  import system.dispatcher

  // #setup
  case class Person(name: String, age: Int, addresses: Seq[Address], isHakker: Boolean)
  case class Address(street: String, city: String, postalCode: Option[Int])
  implicit val addressFormat: BigQueryRootJsonFormat[Address] = bigQueryJsonFormat3(Address.apply)
  implicit val personFormat: BigQueryRootJsonFormat[Person] = bigQueryJsonFormat4(Person.apply)
  // #setup

  @nowarn("msg=dead code")
  val datasetId: String = ???
  @nowarn("msg=dead code")
  val tableId: String = ???

  // #run-query
  val sqlQuery = s"SELECT name, addresses FROM $datasetId.$tableId WHERE age >= 100"
  val centenarians: Source[(String, Seq[Address]), Future[QueryResponse[(String, Seq[Address])]]] =
    BigQuery.query[(String, Seq[Address])](sqlQuery, useLegacySql = false)
  // #run-query

  // #dry-run-query
  val centenariansDryRun = BigQuery.query[(String, Seq[Address])](sqlQuery, dryRun = true, useLegacySql = false)
  val bytesProcessed: Future[Long] = centenariansDryRun.to(Sink.ignore).run().map(_.totalBytesProcessed.get)
  // #dry-run-query

  // #table-data
  val everyone: Source[Person, Future[TableDataListResponse[Person]]] =
    BigQuery.tableData[Person](datasetId, tableId)
  // #table-data

  // #streaming-insert
  val peopleInsertSink: Sink[Seq[Person], NotUsed] =
    BigQuery.insertAll[Person](datasetId, tableId, InsertAllRetryPolicy.WithDeduplication)
  // #streaming-insert

  // #async-insert
  val peopleLoadFlow: Flow[Person, Job, NotUsed] = BigQuery.insertAllAsync[Person](datasetId, tableId)
  // #async-insert

  @nowarn("msg=dead code")
  val people: List[Person] = ???

  // #job-status
  def checkIfJobsDone(jobReferences: Seq[JobReference]): Future[Boolean] = for {
    jobs <- Future.sequence(jobReferences.map(ref => BigQuery.job(ref.jobId.get)))
  } yield jobs.forall(job => job.status.exists(_.state == JobState.Done))

  val isDone: Future[Boolean] = for {
    jobs <- Source(people).via(peopleLoadFlow).runWith(Sink.seq)
    jobReferences = jobs.flatMap(job => job.jobReference)
    isDone <- checkIfJobsDone(jobReferences)
  } yield isDone
  // #job-status

  // #dataset-methods
  val allDatasets: Source[Dataset, NotUsed] = BigQuery.datasets
  val existingDataset: Future[Dataset] = BigQuery.dataset(datasetId)
  val newDataset: Future[Dataset] = BigQuery.createDataset("newDatasetId")
  val datasetDeleted: Future[Done] = BigQuery.deleteDataset(datasetId)
  // #dataset-methods

  // #table-methods
  val allTablesInDataset: Source[Table, Future[TableListResponse]] = BigQuery.tables(datasetId)
  val existingTable: Future[Table] = BigQuery.table(datasetId, tableId)
  val tableDeleted: Future[Done] = BigQuery.deleteTable(datasetId, tableId)
  // #table-methods

  // #create-table
  implicit val addressSchema: TableSchemaWriter[Address] = bigQuerySchema3(Address.apply)
  implicit val personSchema: TableSchemaWriter[Person] = bigQuerySchema4(Person.apply)
  val newTable: Future[Table] = BigQuery.createTable[Person](datasetId, "newTableId")
  // #create-table

  // #custom-settings
  val defaultSettings: GoogleSettings = GoogleSettings()
  val customSettings = defaultSettings.copy(projectId = "myOtherProject")
  BigQuery.query[(String, Seq[Address])](sqlQuery).withAttributes(GoogleAttributes.settings(customSettings))
  // #custom-settings

}
