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

package org.apache.pekko.stream.connectors.xml.scaladsl

import java.nio.charset.{ Charset, StandardCharsets }

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.xml.ParseEvent
import org.apache.pekko.stream.connectors.xml.impl
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString

import javax.xml.stream.XMLOutputFactory

object XmlWriting {

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * @param charset charset of encoding
   */
  def writer(charset: Charset): Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(charset))

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * encoding UTF-8
   * @param xmlOutputFactory factory from which to get an XMLStreamWriter
   */
  def writer(xmlOutputFactory: XMLOutputFactory): Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(StandardCharsets.UTF_8, xmlOutputFactory))

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * @param charset charset of encoding
   * @param xmlOutputFactory factory from which to get an XMLStreamWriter
   */
  def writer(charset: Charset, xmlOutputFactory: XMLOutputFactory): Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(charset, xmlOutputFactory))

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * encoding UTF-8
   */
  val writer: Flow[ParseEvent, ByteString, NotUsed] = writer(StandardCharsets.UTF_8)
}
