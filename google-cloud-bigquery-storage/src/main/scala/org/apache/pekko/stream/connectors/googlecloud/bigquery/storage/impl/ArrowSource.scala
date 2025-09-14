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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.impl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord
import pekko.stream.scaladsl.Source
import com.google.cloud.bigquery.storage.v1.arrow.{ ArrowRecordBatch, ArrowSchema }
import com.google.cloud.bigquery.storage.v1.storage.BigQueryReadClient
import com.google.cloud.bigquery.storage.v1.stream.ReadSession
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.{ VectorLoader, VectorSchemaRoot }
import org.apache.arrow.vector.ipc.ReadChannel
import org.apache.arrow.vector.ipc.message.MessageSerializer
import org.apache.arrow.vector.util.ByteArrayReadableSeekableByteChannel

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

object ArrowSource {

  def readRecordsMerged(client: BigQueryReadClient, readSession: ReadSession): Source[List[BigQueryRecord], NotUsed] =
    readMerged(client, readSession)
      .map(a => new SimpleRowReader(readSession.schema.arrowSchema.get).read(a))

  def readMerged(client: BigQueryReadClient, session: ReadSession): Source[ArrowRecordBatch, NotUsed] =
    read(client, session)
      .reduce((a, b) => a.merge(b))

  def readRecords(client: BigQueryReadClient, session: ReadSession): Seq[Source[BigQueryRecord, NotUsed]] =
    read(client, session)
      .map { a =>
        a.map(new SimpleRowReader(session.schema.arrowSchema.get).read(_))
          .mapConcat(c => c)
      }

  def read(client: BigQueryReadClient, session: ReadSession): Seq[Source[ArrowRecordBatch, NotUsed]] =
    SDKClientSource
      .read(client, session)
      .map { s =>
        s.map(b => b.arrowRecordBatch.toList).mapConcat(a => a)
      }

}

final class SimpleRowReader(val schema: ArrowSchema) extends AutoCloseable {

  val allocator = new RootAllocator(Long.MaxValue)

  val sd = MessageSerializer.deserializeSchema(
    new ReadChannel(
      new ByteArrayReadableSeekableByteChannel(
        schema.serializedSchema.toByteArray)))

  val vec = sd.getFields.asScala.map(_.createVector(allocator))
  var root = new VectorSchemaRoot(vec.asJava)
  val loader = new VectorLoader(root)

  def read(batch: ArrowRecordBatch): List[BigQueryRecord] = {
    val deserializedBatch = MessageSerializer.deserializeRecordBatch(new ReadChannel(
        new ByteArrayReadableSeekableByteChannel(
          batch.serializedRecordBatch.toByteArray)),
      allocator);
    loader.load(deserializedBatch)
    deserializedBatch.close()

    val rs = root.getSchema.getFields
    val fvs = root.getFieldVectors.asScala

    val recordsList = ListBuffer[BigQueryRecord]()
    for (i <- 0 until root.getRowCount) {
      val map = mutable.Map[String, Object]()
      for (fv <- fvs) {
        map.put(rs.get(i).getName, fv.getObject(i))
      }
      recordsList += BigQueryRecord.fromMap(map.toMap)
    }

    root.clear();
    recordsList.toList
  }

  override def close(): Unit = {
    root.close();
    allocator.close();
  }

}
