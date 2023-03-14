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

package org.apache.pekko.stream.connectors.file.impl.archive

import org.apache.pekko.NotUsed
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.file.ArchiveMetadata
import org.apache.pekko.stream.scaladsl.{ Flow, Source }
import org.apache.pekko.util.ByteString

/**
 * INTERNAL API
 */
@InternalApi private[file] object ZipArchiveManager {

  def zipFlow(): Flow[(ArchiveMetadata, Source[ByteString, Any]), ByteString, NotUsed] = {
    val archiveZipFlow = new ZipArchiveFlow()
    Flow[(ArchiveMetadata, Source[ByteString, Any])]
      .flatMapConcat {
        case (metadata, stream) =>
          val prependElem = Source.single(FileByteStringSeparators.createStartingByteString(metadata.filePath))
          val appendElem = Source.single(FileByteStringSeparators.createEndingByteString())
          stream.prepend(prependElem).concat(appendElem)
      }
      .via(archiveZipFlow)
  }

}
