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

import java.net.InetAddress

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.stream.IOResult
import pekko.stream.connectors.ftp.scaladsl.Sftp
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.util.ByteString

import scala.concurrent.Future

trait BaseSftpSpec extends BaseSftpSupport with BaseSpec {

  private def createSettings(credentials: FtpCredentials): SftpSettings =
    SftpSettings(
      InetAddress.getByName(HOSTNAME)).withPort(PORT)
      .withCredentials(credentials)
      .withStrictHostKeyChecking(false)

  val settings = createSettings(CREDENTIALS)
  val wrongSettings = createSettings(WRONG_CREDENTIALS)

  protected def listFilesWithWrongCredentials(basePath: String): Source[FtpFile, NotUsed] =
    Sftp.ls(ROOT_PATH + basePath, wrongSettings)

  protected def listFiles(basePath: String): Source[FtpFile, NotUsed] =
    Sftp.ls(ROOT_PATH + basePath, settings)

  protected def listFilesWithFilter(
      basePath: String,
      branchSelector: FtpFile => Boolean,
      emitTraversedDirectories: Boolean): Source[FtpFile, NotUsed] =
    Sftp.ls(ROOT_PATH + basePath, settings, branchSelector, emitTraversedDirectories)

  protected def retrieveFromPath(
      path: String,
      fromRoot: Boolean = false): Source[ByteString, Future[IOResult]] = {
    val finalPath = if (fromRoot) path else ROOT_PATH + path
    Sftp.fromPath(finalPath, settings)
  }

  protected def retrieveFromPathWithOffset(
      path: String,
      offset: Long): Source[ByteString, Future[IOResult]] =
    Sftp.fromPath(ROOT_PATH + path, settings, 8192, offset)

  protected def storeToPath(path: String, append: Boolean): Sink[ByteString, Future[IOResult]] =
    Sftp.toPath(ROOT_PATH + path, settings, append)

  protected def remove(): Sink[FtpFile, Future[IOResult]] =
    Sftp.remove(settings)

  protected def move(destinationPath: FtpFile => String): Sink[FtpFile, Future[IOResult]] =
    Sftp.move(file => ROOT_PATH + destinationPath(file), settings)

  protected def mkdir(basePath: String, name: String): Source[Done, NotUsed] =
    Sftp.mkdir(ROOT_PATH + basePath, name, settings)
}
