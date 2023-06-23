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

package org.apache.pekko.stream.connectors.ftp.impl

import org.apache.pekko
import pekko.stream.connectors.ftp.RemoteFileSettings
import pekko.stream.impl.Stages.DefaultAttributes.IODispatcher
import pekko.stream.stage.GraphStage
import pekko.stream.{ Attributes, Outlet, SourceShape }

trait FtpGraphStage[FtpClient, S <: RemoteFileSettings, T] extends GraphStage[SourceShape[T]] {
  def name: String

  def basePath: String

  def connectionSettings: S

  def ftpClient: () => FtpClient

  val shape: SourceShape[T] = SourceShape(Outlet[T](s"$name.out"))

  val out: Outlet[T] = shape.outlets.head.asInstanceOf[Outlet[T]]

  override def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name(name) and IODispatcher
}
