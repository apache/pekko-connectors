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

package org.apache.pekko.stream.connectors.s3.impl

import org.apache.pekko
import pekko.stream.scaladsl.Source
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.http.scaladsl.model.{ ContentTypes, HttpEntity, RequestEntity }
import pekko.util.ByteString

import java.io.File

/**
 * Internal Api
 */
@InternalApi private[impl] sealed trait Chunk {
  def asEntity(): RequestEntity
  def size: Int

  /**
   * Releases whatever backs this chunk, once it is known that `data` will not be materialized again.
   * Must not be called while an upload attempt for the chunk may still be retried.
   */
  def dispose(): Unit = ()
}

@InternalApi private[impl] final case class DiskChunk(data: Source[ByteString, NotUsed], size: Int, file: File)
    extends Chunk {
  def asEntity(): RequestEntity = HttpEntity(ContentTypes.`application/octet-stream`, size, data)
  override def dispose(): Unit = { file.delete(): Unit }
}

@InternalApi private[impl] final case class MemoryChunk(data: ByteString) extends Chunk {
  def asEntity(): RequestEntity = HttpEntity.Strict(ContentTypes.`application/octet-stream`, data)
  def size: Int = data.size
}
