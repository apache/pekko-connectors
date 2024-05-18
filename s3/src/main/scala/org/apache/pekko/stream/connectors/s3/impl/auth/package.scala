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

package org.apache.pekko.stream.connectors.s3.impl

import java.security.MessageDigest

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.scaladsl.Flow
import pekko.util.ByteString

package object auth {

  private val Digits = "0123456789abcdef".toCharArray

  @InternalApi private[impl] def encodeHex(bytes: Array[Byte]): String = {
    val length = bytes.length
    val out = new Array[Char](length * 2)
    for (i <- 0 until length) {
      val b = bytes(i)
      out(i * 2) = Digits((b >> 4) & 0xF)
      out(i * 2 + 1) = Digits(b & 0xF)
    }
    new String(out)
  }

  @InternalApi private[impl] def encodeHex(bytes: ByteString): String =
    encodeHex(bytes.toArrayUnsafe())

  @InternalApi private[impl] def digest(algorithm: String = "SHA-256"): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString]
      .fold(MessageDigest.getInstance(algorithm)) {
        case (digest, bytes) =>
          digest.update(bytes.asByteBuffer)
          digest
      }
      .map(d => ByteString.fromArrayUnsafe(d.digest()))
}
