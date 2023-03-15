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

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.csv.impl.CsvParsingStage
import pekko.stream.scaladsl.Flow
import pekko.util.ByteString

object CsvParsing {

  val Backslash: Byte = '\\'
  val Comma: Byte = ','
  val SemiColon: Byte = ';'
  val Colon: Byte = ':'
  val Tab: Byte = '\t'
  val DoubleQuote: Byte = '"'
  val maximumLineLengthDefault: Int = 10 * 1024

  /**
   * Creates CSV parsing flow that reads CSV lines from incoming
   * [[pekko.util.ByteString]] objects.
   */
  def lineScanner(delimiter: Byte = Comma,
      quoteChar: Byte = DoubleQuote,
      escapeChar: Byte = Backslash,
      maximumLineLength: Int = maximumLineLengthDefault): Flow[ByteString, List[ByteString], NotUsed] =
    Flow.fromGraph(new CsvParsingStage(delimiter, quoteChar, escapeChar, maximumLineLength))
}
