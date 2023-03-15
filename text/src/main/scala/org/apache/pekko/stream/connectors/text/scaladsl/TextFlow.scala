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

package org.apache.pekko.stream.connectors.text.scaladsl

import java.nio.charset.Charset

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.text.impl.{ CharsetDecodingFlow, CharsetTranscodingFlow }
import pekko.stream.scaladsl.Flow
import pekko.util.ByteString

/**
 * Scala DSL
 */
object TextFlow {

  /**
   * Decodes a stream of bytes into a stream of characters, using the supplied charset.
   */
  def decoding(incoming: Charset): Flow[ByteString, String, NotUsed] =
    Flow[ByteString]
      .via(new CharsetDecodingFlow(incoming))

  /**
   * Decodes a stream of bytes into a stream of characters, using the supplied charset.
   */
  def encoding(outgoing: Charset): Flow[String, ByteString, NotUsed] =
    Flow[String]
      .map(ByteString(_, outgoing))

  /**
   * Translates a stream of bytes from one character encoding into another.
   */
  def transcoding(incoming: Charset, outgoing: Charset): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString]
      .via(new CharsetTranscodingFlow(incoming, outgoing))

}
