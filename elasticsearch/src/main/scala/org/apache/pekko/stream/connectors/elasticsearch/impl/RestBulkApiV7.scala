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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.elasticsearch.Operation._
import pekko.stream.connectors.elasticsearch.{ MessageWriter, WriteMessage }
import spray.json._

import scala.collection.immutable

/**
 * Internal API.
 *
 * REST API implementation for some Elasticsearch 7 version.
 * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/docs-bulk.html
 */
@InternalApi
private[impl] final class RestBulkApiV7[T, C](indexName: String,
    versionType: Option[String],
    allowExplicitIndex: Boolean,
    messageWriter: MessageWriter[T])
    extends RestBulkApi[T, C] {

  def toJson(messages: immutable.Seq[WriteMessage[T, C]]): String =
    messages
      .map { message =>
        val sharedFields = constructSharedFields(message)
        val tuple: (String, JsObject) = message.operation match {
          case Index =>
            val fields = Seq(
              optionalNumber("version", message.version),
              optionalString("version_type", versionType),
              optionalString("_id", message.id)).flatten
            "index" -> JsObject(sharedFields ++ fields: _*)
          case Create          => "create" -> JsObject(sharedFields ++ optionalString("_id", message.id): _*)
          case Update | Upsert => "update" -> JsObject(sharedFields :+ ("_id" -> JsString(message.id.get)): _*)
          case Delete          =>
            val fields =
              ("_id" -> JsString(message.id.get)) +: Seq(
                optionalNumber("version", message.version),
                optionalString("version_type", versionType)).flatten
            "delete" -> JsObject(sharedFields ++ fields: _*)
          case Nop => "" -> JsObject()
        }
        if (tuple._1.nonEmpty)
          JsObject(tuple).compactPrint + messageToJson(message, message.source.fold("")(messageWriter.convert))
        else
          ""
      }
      .filter(_.nonEmpty) match {
      case Nil => "" // if all NOPs
      case x   => x.mkString("", "\n", "\n")
    }

  override def constructSharedFields(message: WriteMessage[T, C]): Seq[(String, JsString)] = {
    val operationFields = if (allowExplicitIndex) {
      Seq("_index" -> JsString(message.indexName.getOrElse(indexName)))
    } else {
      Seq.empty
    }

    operationFields ++ message.customMetadata.map { case (field, value) => field -> JsString(value) }
  }
}
