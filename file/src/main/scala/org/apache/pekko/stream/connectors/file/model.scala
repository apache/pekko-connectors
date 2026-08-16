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

package org.apache.pekko.stream.connectors.file

import java.time.Instant
import java.time.temporal.ChronoField
import java.util.Objects

/**
 * INTERNAL API
 *
 * Validation for path traversal sequences in archive entry names (Zip Slip / Tar Slip).
 *
 * Both forward slash (`/`) and backslash (`\`) are rejected as path separators because:
 * - ZIP files use forward slashes per the ZIP Application Note (PKWARE) section 4.4.17
 * - TAR file names use forward slashes per POSIX.1 (IEEE Std 1003.1) and the USTAR format
 *   (POSIX.1-2001 / IEEE Std 1003.1-2001, extended by POSIX.1-2008 pax headers)
 * - On Windows, backslashes are path separators and would be interpreted by the filesystem
 *   API when extracting, even though they are not valid separators in the archive formats.
 *   Rejecting them prevents path traversal via crafted archives on Windows hosts.
 */
private[file] object ArchivePathTraversalValidation {

  /**
   * Validate that an archive path segment does not contain traversal sequences.
   * Rejects segments containing `..` as a path component, absolute paths, and
   * backslashes (which Windows treats as path separators during extraction).
   *
   * @param value     the path segment to validate
   * @param fieldName the name of the field for error messages
   * @throws IllegalArgumentException if the segment contains traversal sequences
   */
  def validate(value: String, fieldName: String): Unit = {
    require(value != null, s"$fieldName must not be null")
    // Reject absolute paths
    require(!value.startsWith("/"), s"$fieldName must not be an absolute path: '$value'")
    // Reject backslashes — not valid in ZIP/TAR specs, but treated as path separators on Windows
    require(!value.contains('\\'), s"$fieldName must not contain backslashes: '$value'")
    // Reject path traversal sequences: ".." as a standalone segment
    val segments = value.split('/')
    require(
      !segments.contains(".."),
      s"$fieldName must not contain path traversal sequences: '$value'")
  }
}

final class ArchiveMetadata private (
    val filePath: String)

object ArchiveMetadata {
  def apply(filePath: String): ArchiveMetadata = new ArchiveMetadata(filePath)
  def create(filePath: String): ArchiveMetadata = new ArchiveMetadata(filePath)
}

final case class ZipArchiveMetadata(name: String) {
  ArchivePathTraversalValidation.validate(name, "Zip entry name")
  def getName() = name
}
object ZipArchiveMetadata {
  def create(name: String): ZipArchiveMetadata = ZipArchiveMetadata(name)
}

final class TarArchiveMetadata private (
    val filePathPrefix: Option[String],
    val filePathName: String,
    val size: Long,
    val lastModification: Instant,
    /*
     * See constants `TarchiveMetadata.linkIndicatorNormal`
     */
    val linkIndicatorByte: Byte) {
  val filePath = filePathPrefix match {
    case None         => filePathName
    case Some(prefix) => prefix + "/" + filePathName
  }

  def isDirectory: Boolean =
    linkIndicatorByte == TarArchiveMetadata.linkIndicatorDirectory || filePathName.endsWith("/")

  override def equals(obj: Any): Boolean = {
    obj match {
      case that: TarArchiveMetadata =>
        this.filePathPrefix == that.filePathPrefix &&
        this.filePathName == that.filePathName &&
        this.size == that.size &&
        this.lastModification == that.lastModification &&
        this.linkIndicatorByte == that.linkIndicatorByte
      case _ => false
    }
  }

  override def hashCode(): Int =
    Objects.hash(filePathPrefix, filePathName, Long.box(size), lastModification, Byte.box(linkIndicatorByte))

  override def toString: String =
    "TarArchiveMetadata(" +
    s"filePathPrefix=$filePathPrefix," +
    s"filePathName=$filePathName," +
    s"size=$size," +
    s"lastModification=$lastModification," +
    s"linkIndicatorByte=${linkIndicatorByte.toChar})"
}

object TarArchiveMetadata {

  /**
   * Constants for the `linkIndicator` flag.
   */
  val linkIndicatorNormal: Byte = '0'
  val linkIndicatorLink: Byte = '1'
  val linkIndicatorSymLink: Byte = '2'
  val linkIndicatorCharacterDevice: Byte = '3'
  val linkIndicatorBlockDevice: Byte = '4'
  val linkIndicatorDirectory: Byte = '5'
  val linkIndicatorPipe: Byte = '6'
  val linkIndicatorContiguousFile: Byte = '7'

  def apply(filePath: String, size: Long): TarArchiveMetadata = apply(filePath, size, Instant.now)
  def apply(filePath: String, size: Long, lastModification: Instant): TarArchiveMetadata = {
    val filePathSegments = filePath.lastIndexOf("/")
    val filePathPrefix = if (filePathSegments > 0) {
      Some(filePath.substring(0, filePathSegments))
    } else None
    val filePathName = filePath.substring(filePathSegments + 1, filePath.length)
    apply(filePathPrefix, filePathName, size, lastModification, linkIndicatorNormal)
  }

  def apply(filePathPrefix: String, filePathName: String, size: Long, lastModification: Instant): TarArchiveMetadata = {
    apply(if (filePathPrefix.isEmpty) None else Some(filePathPrefix),
      filePathName,
      size,
      lastModification,
      linkIndicatorNormal)
  }

  /**
   * @param linkIndicatorByte See constants eg. `TarchiveMetadata.linkIndicatorNormal`
   */
  def apply(filePathPrefix: String,
      filePathName: String,
      size: Long,
      lastModification: Instant,
      linkIndicatorByte: Byte): TarArchiveMetadata = {
    apply(if (filePathPrefix.isEmpty) None else Some(filePathPrefix),
      filePathName,
      size,
      lastModification,
      linkIndicatorByte)
  }

  private def apply(filePathPrefix: Option[String],
      filePathName: String,
      size: Long,
      lastModification: Instant,
      linkIndicatorByte: Byte): TarArchiveMetadata = {
    filePathPrefix.foreach { value =>
      require(
        value.length <= 154,
        "File path prefix must be between 1 and 154 characters long")
      ArchivePathTraversalValidation.validate(value, "File path prefix")
    }
    require(filePathName.length >= 0 && filePathName.length <= 99,
      s"File path name must be between 0 and 99 characters long, was ${filePathName.length}")
    ArchivePathTraversalValidation.validate(filePathName, "File path name")

    new TarArchiveMetadata(filePathPrefix,
      filePathName,
      size,
      // tar timestamp granularity is in seconds
      lastModification.`with`(ChronoField.NANO_OF_SECOND, 0L),
      linkIndicatorByte)
  }

  def create(filePath: String, size: Long): TarArchiveMetadata = apply(filePath, size, Instant.now)
  def create(filePath: String, size: Long, lastModification: Instant): TarArchiveMetadata =
    apply(filePath, size, lastModification)
  def create(filePathPrefix: String, filePathName: String, size: Long, lastModification: Instant): TarArchiveMetadata =
    apply(filePathPrefix, filePathName, size, lastModification)

  /**
   * @param linkIndicatorByte See constants eg. `TarchiveMetadata.linkIndicatorNormal`
   */
  def create(filePathPrefix: String,
      filePathName: String,
      size: Long,
      lastModification: Instant,
      linkIndicatorByte: Byte): TarArchiveMetadata =
    apply(filePathPrefix, filePathName, size, lastModification, linkIndicatorByte)

  /**
   * Create metadata for a directory entry.
   */
  def directory(filePathName: String): TarArchiveMetadata =
    directory(filePathName, Instant.now())

  /**
   * Create metadata for a directory entry.
   */
  def directory(filePathName: String, lastModification: Instant): TarArchiveMetadata = {
    val n = if (filePathName.endsWith("/")) filePathName else filePathName + "/"
    apply(None, n, size = 0L, lastModification, linkIndicatorDirectory)
  }

}

final class TarReaderException(msg: String) extends Exception(msg)
