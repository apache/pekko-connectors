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

import net.schmizz.sshj.SSHClient
import org.apache.commons.net.ftp.{ FTPClient, FTPSClient }

import scala.collection.immutable
import scala.util.Try
import java.io.{ InputStream, OutputStream }

import org.apache.pekko.annotation.InternalApi

/**
 * INTERNAL API
 */
@InternalApi
protected[ftp] trait FtpLike[FtpClient, S <: RemoteFileSettings] {

  type Handler

  def connect(connectionSettings: S)(implicit ftpClient: FtpClient): Try[Handler]

  def disconnect(handler: Handler)(implicit ftpClient: FtpClient): Unit

  def listFiles(basePath: String, handler: Handler): immutable.Seq[FtpFile]

  def listFiles(handler: Handler): immutable.Seq[FtpFile]

  def retrieveFileInputStream(name: String, handler: Handler): Try[InputStream]

  def storeFileOutputStream(name: String, handler: Handler, append: Boolean): Try[OutputStream]

  def move(fromPath: String, destinationPath: String, handler: Handler): Unit

  def remove(path: String, handler: Handler): Unit

  def mkdir(path: String, name: String, handler: Handler): Unit
}

/**
 * INTERNAL API
 */
@InternalApi
protected[ftp] trait RetrieveOffset { self: FtpLike[_, _] =>

  def retrieveFileInputStream(name: String, handler: Handler, offset: Long): Try[InputStream]

}

/**
 * INTERNAL API
 */
@InternalApi
protected[ftp] trait UnconfirmedReads { self: FtpLike[_, _] =>

  def retrieveFileInputStream(name: String, handler: Handler, offset: Long, maxUnconfirmedReads: Int): Try[InputStream]

}

/**
 * INTERNAL API
 */
@InternalApi
object FtpLike {
  // type class instances
  implicit val ftpLikeInstance: FtpLike[FTPClient, FtpSettings] with RetrieveOffset with FtpOperations =
    new FtpLike[FTPClient, FtpSettings] with RetrieveOffset with FtpOperations
  implicit val ftpsLikeInstance: FtpLike[FTPSClient, FtpsSettings] with RetrieveOffset with FtpsOperations =
    new FtpLike[FTPSClient, FtpsSettings] with RetrieveOffset with FtpsOperations
  implicit val sFtpLikeInstance
      : FtpLike[SSHClient, SftpSettings] with RetrieveOffset with SftpOperations with UnconfirmedReads =
    new FtpLike[SSHClient, SftpSettings] with RetrieveOffset with SftpOperations with UnconfirmedReads
}
