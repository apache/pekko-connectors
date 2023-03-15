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

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.connectors.file.TarArchiveMetadata
import pekko.stream.scaladsl.{ Flow, Source }
import pekko.util.ByteString

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
