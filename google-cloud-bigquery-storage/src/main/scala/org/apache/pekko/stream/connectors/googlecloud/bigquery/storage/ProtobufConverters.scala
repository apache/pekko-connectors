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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage

import org.apache.pekko
import pekko.annotation.InternalApi
import com.google.cloud.bigquery.storage.v1.ReadSession.TableReadOptions
import com.google.cloud.bigquery.storage.v1.stream.ReadSession
import scalapb.UnknownFieldSet

import scala.jdk.CollectionConverters._

/**
 * Internal API
 */
@InternalApi private[storage] object ProtobufConverters {

  implicit class TableReadOptionsAsScala(val readOption: TableReadOptions) {
    def asScala(): ReadSession.TableReadOptions = {
      ReadSession.TableReadOptions(
        selectedFields = selectedFields(),
        rowRestriction = readOption.getRowRestriction,
        unknownFields = unknownFields())
    }

    private final def selectedFields(): Seq[String] = {
      readOption.getSelectedFieldsList.asScala.map(s => s.asInstanceOf[String]).toSeq
    }

    private final def unknownFields(): scalapb.UnknownFieldSet = {
      val map = readOption.getUnknownFields
        .asMap()
        .asScala
        .map(entry => (entry._1.asInstanceOf[Int], unknownField(entry._2)))
        .toMap
      scalapb.UnknownFieldSet(map)
    }

    private final def unknownField(field: com.google.protobuf.UnknownFieldSet.Field): UnknownFieldSet.Field = {
      UnknownFieldSet.Field(
        varint = field.getVarintList.asScala.map(_.asInstanceOf[Long]).toSeq,
        fixed64 = field.getFixed64List.asScala.map(_.asInstanceOf[Long]).toSeq,
        fixed32 = field.getFixed32List.asScala.map(_.asInstanceOf[Int]).toSeq,
        lengthDelimited = field.getLengthDelimitedList.asScala.toSeq)
    }

  }

}
