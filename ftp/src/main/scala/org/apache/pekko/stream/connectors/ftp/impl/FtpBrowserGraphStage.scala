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

package org.apache.pekko.stream.connectors.ftp
package impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.Attributes
import pekko.stream.stage.OutHandler

/**
 * INTERNAL API
 */
@InternalApi
private[ftp] trait FtpBrowserGraphStage[FtpClient, S <: RemoteFileSettings]
    extends FtpGraphStage[FtpClient, S, FtpFile] {
  val ftpLike: FtpLike[FtpClient, S]

  val branchSelector: FtpFile => Boolean = _ => true

  def emitTraversedDirectories: Boolean = false

  def createLogic(inheritedAttributes: Attributes): FtpGraphStageLogic[FtpFile, FtpClient, S] =
    new FtpGraphStageLogic[FtpFile, FtpClient, S](shape, ftpLike, connectionSettings, ftpClient) {

      private[this] var buffer: Seq[FtpFile] = Seq.empty[FtpFile]

      private[this] var traversed: Seq[FtpFile] = Seq.empty[FtpFile]

      setHandler(
        out,
        new OutHandler {
          def onPull(): Unit = {
            fillBuffer()
            traversed match {
              case head +: tail =>
                traversed = tail
                push(out, head)
              case _ =>
                buffer match {
                  case head +: tail =>
                    buffer = tail
                    push(out, head)
                  case _ => complete(out)
                }
            }
          } // end of onPull

          override def onDownstreamFinish(cause: Throwable): Unit = {
            matSuccess()
            super.onDownstreamFinish(cause)
          }
        }) // end of handler

      protected[this] def doPreStart(): Unit =
        buffer = initBuffer(basePath)

      override protected[this] def matSuccess() = true

      override protected[this] def matFailure(t: Throwable) = true

      private[this] def initBuffer(basePath: String) =
        getFilesFromPath(basePath)

      @scala.annotation.tailrec
      private[this] def fillBuffer(): Unit = buffer match {
        case head +: tail if head.isDirectory && branchSelector(head) =>
          buffer = getFilesFromPath(head.path) ++ tail
          if (emitTraversedDirectories) traversed = traversed :+ head
          fillBuffer()
        case _ => // do nothing
      }

      private[this] def getFilesFromPath(basePath: String) =
        if (basePath.isEmpty)
          graphStageFtpLike.listFiles(handler.get)
        else
          graphStageFtpLike.listFiles(basePath, handler.get)

    } // end of stage logic
}
