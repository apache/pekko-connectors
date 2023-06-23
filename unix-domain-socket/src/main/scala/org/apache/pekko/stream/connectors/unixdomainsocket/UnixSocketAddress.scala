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

package org.apache.pekko.stream.connectors.unixdomainsocket

import java.net.SocketAddress
import java.nio.file.Path

import org.apache.pekko.annotation.InternalApi

object UnixSocketAddress {

  /**
   * Creates an address representing a Unix Domain Socket
   * @param path the path representing the socket
   * @return the address
   */
  def apply(path: Path): UnixSocketAddress =
    new UnixSocketAddress(path)

  /**
   * Java API:
   * Creates an address representing a Unix Domain Socket
   * @param path the path representing the socket
   * @return the address
   */
  def create(path: Path): UnixSocketAddress =
    new UnixSocketAddress(path)
}

/**
 * Represents a path to a file on a file system that a Unix Domain Socket can
 * bind or connect to.
 */
final class UnixSocketAddress @InternalApi private[unixdomainsocket] (val path: Path) extends SocketAddress
