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

package org.apache.pekko.stream.connectors.pravega.impl

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.connectors.pravega.WriterSettings
import org.apache.pekko.stream.stage.StageLogging
@InternalApi private[pravega] trait PravegaWriter extends PravegaCapabilities {
  this: StageLogging =>

  def createWriter[A](streamName: String, writerSettings: WriterSettings[A]) =
    eventStreamClientFactory.createEventWriter(
      streamName,
      writerSettings.serializer,
      writerSettings.eventWriterConfig)

}
