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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl

import org.apache.pekko
import pekko.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import pekko.stream.Materializer
import pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord
import pekko.util.ByteString
import com.google.cloud.bigquery.storage.v1.arrow.ArrowSchema
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.ipc.ReadChannel
import org.apache.arrow.vector.{ VectorLoader, VectorSchemaRoot }
import org.apache.arrow.vector.ipc.message.MessageSerializer
import org.apache.arrow.vector.util.ByteArrayReadableSeekableByteChannel

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._

class ArrowByteStringDecoder(val schema: ArrowSchema) extends FromByteStringUnmarshaller[List[BigQueryRecord]] {

  val allocator = new RootAllocator(Long.MaxValue)

  override def apply(batch: ByteString)(implicit ec: ExecutionContext,
      materializer: Materializer): Future[List[BigQueryRecord]] = {
    val sd = MessageSerializer.deserializeSchema(
      new ReadChannel(
        new ByteArrayReadableSeekableByteChannel(
          schema.serializedSchema.toByteArray)))

    val vec = sd.getFields.asScala.map(_.createVector(allocator))
    val root = new VectorSchemaRoot(vec.asJava)
    val loader = new VectorLoader(root)

    val deserializedBatch = MessageSerializer.deserializeRecordBatch(new ReadChannel(
        new ByteArrayReadableSeekableByteChannel(
          batch.toByteBuffer.array())),
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

    root.close();
    allocator.close();

    Future(recordsList.toList)
  }

}
