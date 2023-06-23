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

package org.apache.pekko.stream.connectors.text.javadsl

import java.nio.charset.Charset

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.text.impl.{ CharsetDecodingFlow, CharsetTranscodingFlow }
import pekko.stream.javadsl.Flow
import pekko.util.ByteString

/**
 * Java DSL
 */
object TextFlow {

  /**
   * Decodes a stream of bytes into a stream of characters, using the supplied charset.
   */
  def decoding(incoming: Charset): Flow[ByteString, String, NotUsed] =
    pekko.stream.scaladsl
      .Flow[ByteString]
      .via(new CharsetDecodingFlow(incoming))
      .asJava

  /**
   * Decodes a stream of bytes into a stream of characters, using the supplied charset.
   */
  def encoding(outgoing: Charset): Flow[String, ByteString, NotUsed] =
    Flow.fromFunction((s: String) => ByteString.fromString(s, outgoing))

  /**
   * Translates a stream of bytes from one character encoding into another.
   */
  def transcoding(incoming: Charset, outgoing: Charset): Flow[ByteString, ByteString, NotUsed] =
    pekko.stream.scaladsl
      .Flow[ByteString]
      .via(new CharsetTranscodingFlow(incoming, outgoing))
      .asJava

}
