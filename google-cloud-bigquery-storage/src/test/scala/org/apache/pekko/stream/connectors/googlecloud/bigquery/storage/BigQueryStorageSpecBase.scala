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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage

import org.apache.pekko
import pekko.Done
import pekko.actor.ActorSystem
import pekko.http.scaladsl.Http
import pekko.stream.connectors.googlecloud.bigquery.storage.mock.{ BigQueryMockData, BigQueryMockServer }
import com.google.cloud.bigquery.storage.v1.storage.ReadRowsResponse.Rows.AvroRows
import com.google.cloud.bigquery.storage.v1.stream.ReadSession.Schema.{ ArrowSchema, AvroSchema }
import com.google.protobuf.ByteString
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Promise
import scala.concurrent.duration._

abstract class BigQueryStorageSpecBase(_port: Int) extends BigQueryMockData with ScalaFutures {
  def this() = this(21000)

  private[bigquery] val bqHost = "localhost"
  private[bigquery] val bqPort = _port

  implicit val system: ActorSystem = ActorSystem("alpakka-bigquery-storage")

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 15.seconds, interval = 50.millis)

  private val binding: Promise[Http.ServerBinding] = Promise[Http.ServerBinding]()

  def storageAvroSchema = {
    AvroSchema(com.google.cloud.bigquery.storage.v1.avro.AvroSchema.of(FullAvroSchema.toString))
  }

  def storageArrowSchema = {
    ArrowSchema(
      com.google.cloud.bigquery.storage.v1.arrow.ArrowSchema.of(ByteString.copyFromUtf8(FullArrowSchema.toJson)))
  }

  def storageAvroRows = {
    AvroRows(recordsAsRows(FullAvroRecord))
  }

  def startMock(): Promise[Http.ServerBinding] = {
    val bindingRes = new BigQueryMockServer(bqPort).run().futureValue
    binding.success(bindingRes)
  }
  def stopMock(): Done = {
    binding.future.futureValue.unbind().futureValue
  }
}
