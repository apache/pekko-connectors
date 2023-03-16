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

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.xml
import pekko.stream.connectors.xml.ParseEvent
import pekko.util.ByteString
import com.fasterxml.aalto.AsyncXMLInputFactory
import org.w3c.dom.Element

import java.util.function.Consumer

import scala.collection.JavaConverters._

object XmlParsing {

  /**
   * Parser Flow that takes a stream of ByteStrings and parses them to XML events similar to SAX.
   */
  def parser(): pekko.stream.javadsl.Flow[ByteString, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.parser.asJava

  /**
   * Parser Flow that takes a stream of ByteStrings and parses them to XML events similar to SAX.
   */
  def parser(ignoreInvalidChars: Boolean): pekko.stream.javadsl.Flow[ByteString, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.parser(ignoreInvalidChars).asJava

  /**
   * Parser Flow that takes a stream of ByteStrings and parses them to XML events similar to SAX.
   */
  def parser(
      configureFactory: Consumer[AsyncXMLInputFactory]): pekko.stream.javadsl.Flow[ByteString, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.parser(false, configureFactory.accept(_)).asJava

  /**
   * Parser Flow that takes a stream of ByteStrings and parses them to XML events similar to SAX.
   */
  def parser(
      ignoreInvalidChars: Boolean,
      configureFactory: Consumer[AsyncXMLInputFactory]): pekko.stream.javadsl.Flow[ByteString, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.parser(ignoreInvalidChars, configureFactory.accept(_)).asJava

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage coalesces consequitive CData and Characters
   * events into a single Characters event or fails if the buffered string is larger than the maximum defined.
   */
  def coalesce(maximumTextLength: Int): pekko.stream.javadsl.Flow[ParseEvent, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.coalesce(maximumTextLength).asJava

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage filters out any event not corresponding to
   * a certain path in the XML document. Any event that is under the specified path (including subpaths) is passed
   * through.
   */
  def subslice(
      path: java.util.Collection[String]): pekko.stream.javadsl.Flow[ParseEvent, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.subslice(path.asScala.toIndexedSeq).asJava

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage pushes elements of a certain path in
   * the XML document as org.w3c.dom.Element.
   */
  def subtree(path: java.util.Collection[String]): pekko.stream.javadsl.Flow[ParseEvent, Element, NotUsed] =
    xml.scaladsl.XmlParsing.subtree(path.asScala.toIndexedSeq).asJava
}
