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
import pekko.annotation.InternalApi
import pekko.stream.connectors.ftp.{ FtpAuthenticationException, FtpsSettings }
import org.apache.commons.net.ftp.{ FTP, FTPClient }

import scala.util.Try

/**
 * INTERNAL API
 */
@InternalApi
private[ftp] trait FtpsOperations extends CommonFtpOperations {
  self: FtpLike[FTPClient, FtpsSettings] =>

  def connect(connectionSettings: FtpsSettings)(implicit ftpClient: FTPClient): Try[Handler] =
    Try {
      connectionSettings.proxy.foreach(ftpClient.setProxy)

      ftpClient match {
        case legacyClient: LegacyFtpsClient =>
          connectionSettings.keyManager.foreach(legacyClient.setKeyManager)
          connectionSettings.trustManager.foreach(legacyClient.setTrustManager)
        case _ =>
      }

      if (ftpClient.getAutodetectUTF8() != connectionSettings.autodetectUTF8) {
        ftpClient.setAutodetectUTF8(connectionSettings.autodetectUTF8)
      }

      ftpClient.connect(connectionSettings.host, connectionSettings.port)

      connectionSettings.configureConnection(ftpClient)

      ftpClient.login(
        connectionSettings.credentials.username,
        connectionSettings.credentials.password)
      if (ftpClient.getReplyCode == 530) {
        throw new FtpAuthenticationException(
          s"unable to login to host=[${connectionSettings.host}], port=${connectionSettings.port} ${connectionSettings.proxy
              .fold("")("proxy=" + _.toString)}")
      }

      if (connectionSettings.binary) {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
      }

      if (connectionSettings.passiveMode) {
        ftpClient.enterLocalPassiveMode()
      }

      ftpClient
    }

  def disconnect(handler: Handler)(implicit ftpClient: FTPClient): Unit =
    if (ftpClient.isConnected) {
      ftpClient.logout()
      ftpClient.disconnect()
    }
}
