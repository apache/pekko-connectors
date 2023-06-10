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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.elasticsearch.Operation.{ Create, Delete, Index, Nop, Update, Upsert }
import pekko.stream.connectors.elasticsearch.{ WriteMessage, WriteResult }
import spray.json._

import scala.collection.immutable

/**
 * Internal API.
 */
@InternalApi
private[impl] abstract class RestBulkApi[T, C] {

  def toJson(messages: immutable.Seq[WriteMessage[T, C]]): String

  def toWriteResults(messages: immutable.Seq[WriteMessage[T, C]],
      jsonString: String): immutable.Seq[WriteResult[T, C]] = {
    val responseJson = jsonString.parseJson

    // If some commands in bulk request failed, pass failed messages to follows.
    val items = responseJson.asJsObject.fields("items").asInstanceOf[JsArray]
    buildMessageResults(items, messages)
  }

  def optionalString(fieldName: String, value: Option[String]): Option[(String, JsString)] =
    value.map(v => fieldName -> JsString(v))

  def optionalNumber(fieldName: String, value: Option[Long]): Option[(String, JsNumber)] =
    value.map(v => fieldName -> JsNumber(v))

  def messageToJson(message: WriteMessage[T, C], messageSource: String): String = message.operation match {
    case Index | Create => "\n" + messageSource
    case Upsert         => "\n" + JsObject("doc" -> messageSource.parseJson, "doc_as_upsert" -> JsTrue).compactPrint
    case Update         => "\n" + JsObject("doc" -> messageSource.parseJson).compactPrint
    case Delete         => ""
    case Nop            => ""
  }

  def constructSharedFields(message: WriteMessage[T, C]): Seq[(String, JsString)]

  /** NOPs don't come back so slip them into the results like this: */
  private def buildMessageResults(items: JsArray,
      messages: immutable.Seq[WriteMessage[T, C]]): immutable.Seq[WriteResult[T, C]] = {
    val ret = new immutable.VectorBuilder[WriteResult[T, C]]
    ret.sizeHint(messages)
    val itemsIter = items.elements.iterator
    messages.foreach { message =>
      if (message.operation == Nop) {
        // client just wants to pass-through:
        ret += new WriteResult(message, None)
      } else {
        if (itemsIter.hasNext) {
          // good message
          val command = message.operation.command
          val res = itemsIter.next().asJsObject.fields(command).asJsObject
          val error: Option[String] = res.fields.get("error").map(_.compactPrint)
          ret += new WriteResult(message, error)
        } else {
          // error?
          ret += new WriteResult(message, None)
        }
      }
    }
    ret.result()
  }
}
