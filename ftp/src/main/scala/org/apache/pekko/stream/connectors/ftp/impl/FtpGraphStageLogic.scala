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

package org.apache.pekko.stream.connectors.ftp
package impl

import java.io.IOException

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.Shape
import pekko.stream.stage.GraphStageLogic

import scala.util.control.NonFatal

/**
 * INTERNAL API
 */
@InternalApi
private[ftp] abstract class FtpGraphStageLogic[T, FtpClient, S <: RemoteFileSettings](
    val shape: Shape,
    val ftpLike: FtpLike[FtpClient, S],
    val connectionSettings: S,
    val ftpClient: () => FtpClient) extends GraphStageLogic(shape) {

  protected[this] implicit val client = ftpClient()
  protected[this] var handler: Option[ftpLike.Handler] = Option.empty[ftpLike.Handler]
  protected[this] var failed = false

  override def preStart(): Unit = {
    super.preStart()
    try {
      val tryConnect = ftpLike.connect(connectionSettings)
      if (tryConnect.isSuccess) {
        handler = tryConnect.toOption
      } else
        tryConnect.failed.foreach {
          case NonFatal(t) => throw t
          case _           =>
        }
      doPreStart()
    } catch {
      case NonFatal(t) =>
        matFailure(t)
        failStage(t)
    }
  }

  override def postStop(): Unit = {
    try {
      disconnect()
    } catch {
      case e: IOException =>
        matFailure(e)
        // If we're failing, we might not be able to cleanly shut down the connection.
        // So swallow any IO exceptions
        if (!failed) throw e
      case NonFatal(e) =>
        matFailure(e)
        throw e
    }
    matSuccess()
    super.postStop()
  }

  protected[this] def doPreStart(): Unit

  protected[this] def disconnect(): Unit =
    handler.foreach(ftpLike.disconnect)

  protected[this] def matSuccess(): Boolean

  protected[this] def matFailure(t: Throwable): Boolean

}
