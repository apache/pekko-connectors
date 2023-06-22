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

package org.apache.pekko.stream.connectors.file.impl.archive

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.util.ByteString

/**
 * INTERNAL API
 *
 * ArchiveZipFlow operates on ByteString. But it is required to inform ZipOutputStream when each file starts and ends.
 * For this, special starting and ending ByteString is added.
 */
@InternalApi private[file] object FileByteStringSeparators {
  private val startFileWord = "$START$"
  private val endFileWord = "$END$"
  private val separator: Char = '|'

  def createStartingByteString(path: String): ByteString =
    ByteString(s"$startFileWord$separator$path")

  def createEndingByteString(): ByteString =
    ByteString(endFileWord)

  def isStartingByteString(b: ByteString): Boolean =
    b.utf8String.startsWith(startFileWord)

  def isEndingByteString(b: ByteString): Boolean =
    b.utf8String == endFileWord

  def getPathFromStartingByteString(b: ByteString): String = {
    val splitted = b.utf8String.split(separator)
    if (splitted.length == 1) {
      ""
    } else if (splitted.length == 2) {
      splitted.tail.head
    } else {
      splitted.tail.mkString(separator.toString)
    }
  }
}
