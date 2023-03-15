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

package org.apache.pekko.stream.connectors.xml.javadsl

import java.nio.charset.{ Charset, StandardCharsets }

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.xml.ParseEvent
import pekko.stream.connectors.xml.impl
import pekko.stream.scaladsl.Flow
import pekko.util.ByteString

import javax.xml.stream.XMLOutputFactory

object XmlWriting {

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * encoding UTF-8
   */
  def writer(): pekko.stream.javadsl.Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(StandardCharsets.UTF_8)).asJava

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * @param charset encoding of the stream
   */
  def writer(charset: Charset): pekko.stream.javadsl.Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(charset)).asJava

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * @param xmlOutputFactory factory from which to get an XMLStreamWriter
   */
  def writer(
      xmlOutputFactory: XMLOutputFactory): pekko.stream.javadsl.Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(StandardCharsets.UTF_8, xmlOutputFactory)).asJava

  /**
   * Writer Flow that takes a stream of XML events similar to SAX and write ByteStrings.
   * @param charset encoding of the stream
   * @param xmlOutputFactory factory from which to get an XMLStreamWriter
   */
  def writer(charset: Charset,
      xmlOutputFactory: XMLOutputFactory): pekko.stream.javadsl.Flow[ParseEvent, ByteString, NotUsed] =
    Flow.fromGraph(new impl.StreamingXmlWriter(charset, xmlOutputFactory)).asJava

}
