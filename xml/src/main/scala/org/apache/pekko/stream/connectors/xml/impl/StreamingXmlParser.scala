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

package org.apache.pekko.stream.connectors.xml.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import pekko.stream.connectors.xml._
import pekko.stream.connectors.xml.impl.StreamingXmlParser.withStreamingFinishedException
import pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import pekko.util.ByteString
import com.fasterxml.aalto.{ AsyncByteArrayFeeder, AsyncXMLInputFactory, AsyncXMLStreamReader }
import com.fasterxml.aalto.stax.InputFactoryImpl
import com.fasterxml.aalto.util.IllegalCharHandler.ReplacingIllegalCharHandler

import scala.annotation.tailrec

private[xml] object StreamingXmlParser {
  lazy val withStreamingFinishedException = new IllegalStateException("Stream finished before event was fully parsed.")
}

/**
 * INTERNAL API
 */
@InternalApi private[xml] class StreamingXmlParser(ignoreInvalidChars: Boolean,
    configureFactory: AsyncXMLInputFactory => Unit)
    extends GraphStage[FlowShape[ByteString, ParseEvent]] {
  val in: Inlet[ByteString] = Inlet("XMLParser.in")
  val out: Outlet[ParseEvent] = Outlet("XMLParser.out")
  override val shape: FlowShape[ByteString, ParseEvent] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      private var started: Boolean = false

      import javax.xml.stream.XMLStreamConstants

      private val factory: AsyncXMLInputFactory = new InputFactoryImpl()
      configureFactory(factory)
      private val parser: AsyncXMLStreamReader[AsyncByteArrayFeeder] = factory.createAsyncForByteArray()
      if (ignoreInvalidChars) {
        parser.getConfig.setIllegalCharHandler(new ReplacingIllegalCharHandler(0))
      }

      setHandlers(in, out, this)

      override def onPush(): Unit = {
        val array = grab(in).toArray
        parser.getInputFeeder.feedInput(array, 0, array.length)
        advanceParser()
      }

      override def onPull(): Unit = advanceParser()

      override def onUpstreamFinish(): Unit = {
        parser.getInputFeeder.endOfInput()
        if (!parser.hasNext) completeStage()
        else if (isAvailable(out)) advanceParser()
      }

      @tailrec private def advanceParser(): Unit =
        if (parser.hasNext) {
          parser.next() match {
            case AsyncXMLStreamReader.EVENT_INCOMPLETE if isClosed(in) && !started => completeStage()
            case AsyncXMLStreamReader.EVENT_INCOMPLETE if isClosed(in)             => failStage(withStreamingFinishedException)
            case AsyncXMLStreamReader.EVENT_INCOMPLETE                             => pull(in)

            case XMLStreamConstants.START_DOCUMENT =>
              started = true
              push(out, StartDocument)

            case XMLStreamConstants.END_DOCUMENT =>
              push(out, EndDocument)
              completeStage()

            case XMLStreamConstants.START_ELEMENT =>
              val attributes = (0 until parser.getAttributeCount).map { i =>
                val optNs = Option(parser.getAttributeNamespace(i)).filterNot(_ == "")
                val optPrefix = Option(parser.getAttributePrefix(i)).filterNot(_ == "")
                Attribute(name = parser.getAttributeLocalName(i),
                  value = parser.getAttributeValue(i),
                  prefix = optPrefix,
                  namespace = optNs)
              }.toList
              val namespaces = (0 until parser.getNamespaceCount).map { i =>
                val namespace = parser.getNamespaceURI(i)
                val optPrefix = Option(parser.getNamespacePrefix(i)).filterNot(_ == "")
                Namespace(namespace, optPrefix)
              }.toList
              val optPrefix = Option(parser.getPrefix)
              val optNs = optPrefix.flatMap(prefix => Option(parser.getNamespaceURI(prefix)))
              push(
                out,
                StartElement(parser.getLocalName,
                  attributes,
                  optPrefix.filterNot(_ == ""),
                  optNs.filterNot(_ == ""),
                  namespaceCtx = namespaces))

            case XMLStreamConstants.END_ELEMENT =>
              push(out, EndElement(parser.getLocalName))

            case XMLStreamConstants.CHARACTERS =>
              push(out, Characters(parser.getText))

            case XMLStreamConstants.PROCESSING_INSTRUCTION =>
              push(out, ProcessingInstruction(Option(parser.getPITarget), Option(parser.getPIData)))

            case XMLStreamConstants.COMMENT =>
              push(out, Comment(parser.getText))

            case XMLStreamConstants.CDATA =>
              push(out, CData(parser.getText))

            // Do not support DTD, SPACE, NAMESPACE, NOTATION_DECLARATION, ENTITY_DECLARATION, PROCESSING_INSTRUCTION
            // ATTRIBUTE is handled in START_ELEMENT implicitly

            case x =>
              advanceParser()
          }
        } else completeStage()
    }
}
