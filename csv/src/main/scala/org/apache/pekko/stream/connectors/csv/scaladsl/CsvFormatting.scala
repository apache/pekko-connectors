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

package org.apache.pekko.stream.connectors.csv.scaladsl

import java.nio.charset.{ Charset, StandardCharsets }

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.csv.impl.CsvFormatter
import org.apache.pekko.stream.scaladsl.{ Flow, Source }
import org.apache.pekko.util.ByteString

import scala.collection.immutable

/**
 * Provides CSV formatting flows that convert a sequence of String into their CSV representation
 * in [[org.apache.pekko.util.ByteString]].
 */
object CsvFormatting {

  val Backslash: Char = '\\'
  val Comma: Char = ','
  val SemiColon: Char = ';'
  val Colon: Char = ':'
  val Tab: Char = '\t'
  val DoubleQuote: Char = '"'

  /**
   * Create a Flow for converting iterables to ByteString.
   * @param endOfLine Line ending (default CR, LF)
   * @param quotingStyle Quote all fields, or only fields requiring quotes (default)
   * @param charset Character set, defaults to UTF-8
   * @param byteOrderMark Certain CSV readers (namely Microsoft Excel) require a Byte Order mark, defaults to None
   */
  def format[T <: immutable.Iterable[String]](
      delimiter: Char = Comma,
      quoteChar: Char = DoubleQuote,
      escapeChar: Char = Backslash,
      endOfLine: String = "\r\n",
      quotingStyle: CsvQuotingStyle = CsvQuotingStyle.Required,
      charset: Charset = StandardCharsets.UTF_8,
      byteOrderMark: Option[ByteString] = None): Flow[T, ByteString, NotUsed] = {
    val formatter =
      new CsvFormatter(delimiter, quoteChar, escapeChar, endOfLine, quotingStyle, charset)
    byteOrderMark.fold {
      Flow[T].map(formatter.toCsv).named("CsvFormatting")
    } { bom =>
      Flow[T].map(formatter.toCsv).named("CsvFormatting").prepend(Source.single(bom))
    }

  }
}
