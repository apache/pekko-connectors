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

import java.time.{ Instant, LocalDateTime, ZoneId, ZonedDateTime }

import org.apache.pekko
import pekko.stream.connectors.file.TarArchiveMetadata
import pekko.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TarArchiveEntrySpec extends AnyFlatSpec with Matchers {

  "Path traversal validation" should "reject dot-dot in filename" in {
    an[IllegalArgumentException] should be thrownBy {
      TarArchiveMetadata("../etc/passwd", 100L)
    }
  }

  it should "reject dot-dot in prefix" in {
    an[IllegalArgumentException] should be thrownBy {
      TarArchiveMetadata("../../etc", "passwd", 100L, Instant.now)
    }
  }

  it should "reject absolute path in filename" in {
    an[IllegalArgumentException] should be thrownBy {
      TarArchiveMetadata("/etc/passwd", 100L)
    }
  }

  it should "reject dot-dot in middle of path" in {
    an[IllegalArgumentException] should be thrownBy {
      TarArchiveMetadata("dir/../../../etc/passwd", 100L)
    }
  }

  it should "reject dot-dot via parse" in {
    // Build a tar header with a malicious filename
    val malicious = TarArchiveMetadata("dir/file.txt", 100L)
    val entry = new TarArchiveEntry(malicious)
    val header = entry.headerBytes
    // Corrupt the filename field to contain ../
    val corrupted = header.toArray
    val evilName = "../etc/crontab"
    evilName.getBytes.zipWithIndex.foreach { case (b, i) => corrupted(i) = b }
    corrupted(evilName.length) = 0 // null terminator
    an[IllegalArgumentException] should be thrownBy {
      TarArchiveEntry.parse(ByteString(corrupted))
    }
  }

  it should "accept normal relative paths" in {
    val meta = TarArchiveMetadata("dir/subdir/file.txt", 100L)
    meta.filePath shouldBe "dir/subdir/file.txt"
  }

  it should "accept simple filename" in {
    val meta = TarArchiveMetadata("file.txt", 100L)
    meta.filePath shouldBe "file.txt"
  }

  "Metadata entries" should "be created and parsed back" in {
    val filePathPrefix = "dir1/dir2"
    val filename = "thefile.txt"
    val size = 100
    val lastModified = Instant.from(ZonedDateTime.of(LocalDateTime.of(2020, 4, 11, 11, 34), ZoneId.of("CET")))
    val data =
      TarArchiveMetadata(filePathPrefix, filename, size, lastModified, TarArchiveMetadata.linkIndicatorDirectory)
    val entry = new TarArchiveEntry(data)
    val header = entry.headerBytes

    val parsed = TarArchiveEntry.parse(header)
    parsed.filePath shouldBe filePathPrefix + "/" + filename
    parsed.size shouldBe size
    parsed.lastModification shouldBe lastModified
    parsed.isDirectory shouldBe true
  }

  "Header parser" should "handle both space and null character as terminal" in {
    val filePathPrefix = "dir1/dir2"
    val filename = "thefile.txt"
    val size = 100
    val lastModified = Instant.from(ZonedDateTime.of(LocalDateTime.of(2020, 4, 11, 11, 34), ZoneId.of("CET")))
    val data = TarArchiveMetadata(filePathPrefix, filename, size, lastModified, TarArchiveMetadata.linkIndicatorNormal)
    val entry = new TarArchiveEntry(data)

    val headerWithNull = entry.headerBytes
    // Change terminal character after size and lastModified field to be space instead of null
    val bytesWithSpace = headerWithNull.toArray.updated(135, ' '.toByte).updated(147, ' '.toByte)
    val headerWithSpace = ByteString(bytesWithSpace)

    val parsedNull = TarArchiveEntry.parse(headerWithNull)
    val parsedSpace = TarArchiveEntry.parse(headerWithSpace)

    parsedNull shouldBe parsedSpace
  }
}
