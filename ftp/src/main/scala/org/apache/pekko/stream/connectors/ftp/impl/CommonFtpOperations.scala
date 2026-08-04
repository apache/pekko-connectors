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

import java.io.{ IOException, InputStream, OutputStream }
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.TimeZone

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.ftp.FtpFile
import org.apache.commons.net.ftp.{ FTPClient, FTPFile }

import scala.collection.immutable
import scala.util.Try

/**
 * INTERNAL API
 */
@InternalApi
private[ftp] trait CommonFtpOperations {
  type Handler = FTPClient

  def listFiles(basePath: String, handler: Handler): immutable.Seq[FtpFile] = {
    val path = if (basePath.nonEmpty && basePath.head != '/') s"/$basePath" else if (basePath == "/") "" else basePath
    handler
      .listFiles(path)
      .collect {
        case file: FTPFile if file.getName != "." && file.getName != ".." =>
          val calendar = file.getTimestamp
          calendar.setTimeZone(TimeZone.getTimeZone("UTC"))
          FtpFile(
            file.getName,
            if (java.io.File.separatorChar == '\\')
              Paths.get(s"$path/${file.getName}").normalize.toString.replace('\\', '/')
            else
              Paths.get(s"$path/${file.getName}").normalize.toString,
            file.isDirectory,
            file.getSize,
            calendar.getTimeInMillis,
            getPosixFilePermissions(file))
      }
      .toVector
  }

  private def getPosixFilePermissions(file: FTPFile) =
    Map(
      PosixFilePermission.OWNER_READ -> file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION),
      PosixFilePermission.OWNER_WRITE -> file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION),
      PosixFilePermission.OWNER_EXECUTE -> file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION),
      PosixFilePermission.GROUP_READ -> file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION),
      PosixFilePermission.GROUP_WRITE -> file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION),
      PosixFilePermission.GROUP_EXECUTE -> file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION),
      PosixFilePermission.OTHERS_READ -> file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION),
      PosixFilePermission.OTHERS_WRITE -> file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION),
      PosixFilePermission.OTHERS_EXECUTE -> file.hasPermission(FTPFile.WORLD_ACCESS,
        FTPFile.EXECUTE_PERMISSION)).collect {
      case (perm, true) => perm
    }.toSet

  def listFiles(handler: Handler): immutable.Seq[FtpFile] = listFiles("", handler)

  def retrieveFileInputStream(name: String, handler: Handler): Try[InputStream] =
    retrieveFileInputStream(name, handler, 0L)

  def retrieveFileInputStream(name: String, handler: Handler, offset: Long): Try[InputStream] = Try {
    CommonFtpOperations.validatePath(name, "name")
    handler.setRestartOffset(offset)
    val is = handler.retrieveFileStream(name)
    if (is != null) is else throw new IOException(s"$name: No such file or directory")
  }

  def storeFileOutputStream(name: String, handler: Handler, append: Boolean): Try[OutputStream] = Try {
    CommonFtpOperations.validatePath(name, "name")
    val os = if (append) handler.appendFileStream(name) else handler.storeFileStream(name)
    if (os != null) os else throw new IOException(s"Could not write to $name")
  }

  def move(fromPath: String, destinationPath: String, handler: Handler): Unit = {
    CommonFtpOperations.validatePath(fromPath, "fromPath")
    CommonFtpOperations.validatePath(destinationPath, "destinationPath")
    if (!handler.rename(fromPath, destinationPath)) throw new IOException(s"Could not move $fromPath")
  }

  def remove(path: String, handler: Handler): Unit = {
    CommonFtpOperations.validatePath(path, "path")
    if (!handler.deleteFile(path)) throw new IOException(s"Could not delete $path")
  }

  def completePendingCommand(handler: Handler): Boolean =
    handler.completePendingCommand()

  def mkdir(path: String, name: String, handler: Handler): Unit = {
    val updatedPath = CommonFtpOperations.concatPath(path, name)
    handler.makeDirectory(updatedPath)

    if (handler.getReplyCode != 257) {
      throw new IOException(handler.getReplyString)
    }
  }
}

private[ftp] object CommonFtpOperations {

  /**
   * Normalize a path to use `/` separators. FTP uses `/` by protocol;
   * normalizing early ensures all downstream checks only need to handle `/`.
   */
  private def normalizeSeparators(path: String): String = path.replace('\\', '/')

  /**
   * Validate that a path does not contain traversal sequences (`..`).
   * Rejects null values and paths containing `..` as a path segment.
   * Accepts both `/` and `\` separators; backslashes are normalized to `/` before checking.
   *
   * @param path      the path to validate
   * @param fieldName the name of the field for error messages
   * @throws IllegalArgumentException if the path contains traversal sequences
   */
  def validatePath(path: String, fieldName: String): Unit = {
    require(path != null, s"$fieldName must not be null")
    val normalized = normalizeSeparators(path)
    val segments = normalized.split('/')
    require(!segments.contains(".."), s"$fieldName must not contain path traversal sequences: '$path'")
  }

  def concatPath(path: String, name: String): String = {
    validatePath(name, "name")
    val normName = normalizeSeparators(name)
    require(!normName.startsWith("/"), s"name must not be an absolute path: '$normName'")

    require(path != null, "path must not be null")
    val normPath = normalizeSeparators(path)
    val result = if (normPath.endsWith("/")) {
      normPath ++ normName
    } else {
      s"$normPath/$normName"
    }

    // Validate the normalized result doesn't escape the base path
    val normalized = java.nio.file.Paths.get(result).normalize().toString
    val normalizedBase = java.nio.file.Paths.get(normPath).normalize().toString
    // On Windows, Paths.get normalizes to backslash; compare with forward-slash versions
    val normalizedFwd = normalized.replace('\\', '/')
    val normalizedBaseFwd = normalizedBase.replace('\\', '/')
    require(
      normalizedFwd.startsWith(normalizedBaseFwd),
      s"concatPath result '$result' escapes base path '$normPath' after normalization")

    result
  }
}
