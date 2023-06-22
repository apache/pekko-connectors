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

package org.apache.pekko.stream.connectors.xml.impl

import java.nio.charset.Charset

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.{ Attributes, FlowShape, Inlet, Outlet }
import pekko.stream.connectors.xml._
import pekko.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import pekko.util.{ ByteString, ByteStringBuilder }
import javax.xml.stream.XMLOutputFactory

/**
 * INTERNAL API
 */
@InternalApi private[xml] class StreamingXmlWriter(charset: Charset, xmlOutputFactory: XMLOutputFactory)
    extends GraphStage[FlowShape[ParseEvent, ByteString]] {

  def this(charset: Charset) = this(charset, XMLOutputFactory.newInstance())

  val in: Inlet[ParseEvent] = Inlet("XMLWriter.in")
  val out: Outlet[ByteString] = Outlet("XMLWriter.out")
  override val shape: FlowShape[ParseEvent, ByteString] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      val byteStringBuilder = new ByteStringBuilder()

      val output = xmlOutputFactory.createXMLStreamWriter(byteStringBuilder.asOutputStream, charset.name())

      setHandlers(in, out, this)

      def writeAttributes(attributes: List[Attribute]): Unit =
        attributes.foreach { att =>
          att match {
            case Attribute(name, value, Some(prefix), Some(namespace)) =>
              output.writeAttribute(prefix, namespace, name, value)
            case Attribute(name, value, None, Some(namespace)) =>
              output.writeAttribute(namespace, name, value)
            case Attribute(name, value, Some(_), None) =>
              output.writeAttribute(name, value)
            case Attribute(name, value, None, None) =>
              output.writeAttribute(name, value)
          }

        }

      override def onPush(): Unit = {
        val ev: ParseEvent = grab(in)
        ev match {
          case StartDocument =>
            output.writeStartDocument()

          case EndDocument =>
            output.writeEndDocument()

          case StartElement(localName, attributes, optPrefix, Some(namespace), namespaceCtx) =>
            val prefix = optPrefix.getOrElse("")
            output.setPrefix(prefix, namespace)
            output.writeStartElement(prefix, localName, namespace)
            namespaceCtx.foreach(ns => output.writeNamespace(ns.prefix.getOrElse(""), ns.uri))
            writeAttributes(attributes)

          case StartElement(localName, attributes, Some(_), None, namespaceCtx) => // Shouldn't happened
            output.writeStartElement(localName)
            namespaceCtx.foreach(ns => output.writeNamespace(ns.prefix.getOrElse(""), ns.uri))
            writeAttributes(attributes)

          case StartElement(localName, attributes, None, None, namespaceCtx) =>
            output.writeStartElement(localName)
            namespaceCtx.foreach(ns => output.writeNamespace(ns.prefix.getOrElse(""), ns.uri))
            writeAttributes(attributes)

          case EndElement(_) =>
            output.writeEndElement()

          case Characters(text) =>
            output.writeCharacters(text)
          case ProcessingInstruction(Some(target), Some(data)) =>
            output.writeProcessingInstruction(target, data)

          case ProcessingInstruction(Some(target), None) =>
            output.writeProcessingInstruction(target)

          case ProcessingInstruction(None, Some(data)) =>
            output.writeProcessingInstruction(None.orNull, data)
          case ProcessingInstruction(None, None) =>
          case Comment(text) =>
            output.writeComment(text)

          case CData(text) =>
            output.writeCData(text)
        }
        push(out, byteStringBuilder.result().compact)
        byteStringBuilder.clear()
      }

      override def onPull(): Unit = pull(in)

      override def onUpstreamFinish(): Unit = {
        output.flush()
        val finalData = byteStringBuilder.result().compact
        if (finalData.length != 0) {
          emit(out, finalData)
        }
        super.onUpstreamFinish()
      }
    }
}
