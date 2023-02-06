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

import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.{ Sink, Source }
import org.apache.pekko.stream.connectors.ftp.scaladsl.Ftp
import org.apache.pekko.util.ByteString

import scala.concurrent.Future
import java.net.InetAddress

trait BaseFtpSpec extends BaseFtpSupport with BaseSpec {

  private def createSettings(credentials: FtpCredentials): FtpSettings =
    FtpSettings(
      InetAddress.getByName(HOSTNAME)).withPort(PORT)
      .withCredentials(credentials)
      .withBinary(true)
      .withPassiveMode(true)

  val settings = createSettings(CREDENTIALS)
  val wrongSettings = createSettings(WRONG_CREDENTIALS)

  protected def listFilesWithWrongCredentials(basePath: String): Source[FtpFile, NotUsed] =
    Ftp.ls(basePath, wrongSettings)

  protected def listFiles(basePath: String): Source[FtpFile, NotUsed] =
    Ftp.ls(basePath, settings)

  protected def listFilesWithFilter(
      basePath: String,
      branchSelector: FtpFile => Boolean,
      emitTraversedDirectories: Boolean = false): Source[FtpFile, NotUsed] =
    Ftp.ls(basePath, settings, branchSelector, emitTraversedDirectories)

  protected def retrieveFromPath(
      path: String,
      fromRoot: Boolean = false): Source[ByteString, Future[IOResult]] =
    Ftp.fromPath(path, settings)

  protected def retrieveFromPathWithOffset(
      path: String,
      offset: Long): Source[ByteString, Future[IOResult]] =
    Ftp.fromPath(path, settings, 8192, offset)

  protected def storeToPath(path: String, append: Boolean): Sink[ByteString, Future[IOResult]] =
    Ftp.toPath(path, settings, append)

  protected def remove(): Sink[FtpFile, Future[IOResult]] =
    Ftp.remove(settings)

  protected def move(destinationPath: FtpFile => String): Sink[FtpFile, Future[IOResult]] =
    Ftp.move(destinationPath, settings)

  protected def mkdir(basePath: String, name: String): Source[Done, NotUsed] =
    Ftp.mkdir(basePath, name, settings)
}
