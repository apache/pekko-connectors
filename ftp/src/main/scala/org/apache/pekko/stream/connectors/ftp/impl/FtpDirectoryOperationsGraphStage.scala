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

package org.apache.pekko.stream.connectors.ftp.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.ftp.RemoteFileSettings
import pekko.stream.stage.{ GraphStageLogic, OutHandler }
import pekko.stream.Attributes

@InternalApi
private[ftp] trait FtpDirectoryOperationsGraphStage[FtpClient, S <: RemoteFileSettings]
    extends FtpGraphStage[FtpClient, S, Unit] {
  val ftpLike: FtpLike[FtpClient, S]
  val directoryName: String

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new FtpGraphStageLogic(shape, ftpLike, connectionSettings, ftpClient) {
      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            push(out, graphStageFtpLike.mkdir(basePath, directoryName, handler.get))
            complete(out)
          }
        })

      override protected def doPreStart(): Unit = ()

      override protected def matSuccess(): Boolean = true

      override protected def matFailure(t: Throwable): Boolean = true
    }
}
