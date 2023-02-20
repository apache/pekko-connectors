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

package akka.stream.alpakka.file.impl.archive

import akka.NotUsed
import akka.annotation.InternalApi
import akka.stream.alpakka.file.TarArchiveMetadata
import akka.stream.scaladsl.{ Flow, Source }
import akka.util.ByteString

/**
 * INTERNAL API
 */
@InternalApi private[file] object TarArchiveManager {

  def tarFlow(): Flow[(TarArchiveMetadata, Source[ByteString, _]), ByteString, NotUsed] = {
    Flow[(TarArchiveMetadata, Source[ByteString, Any])]
      .flatMapConcat {
        case (metadata, stream) =>
          val entry = new TarArchiveEntry(metadata)
          Source
            .single(entry.headerBytes)
            .concat(stream.via(new EnsureByteStreamSize(metadata.size)))
            .concat(Source.single(entry.trailingBytes))
      }
  }

}
