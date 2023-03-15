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

package docs.scaladsl

import java.nio.charset.StandardCharsets

import org.apache.pekko
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.util.ByteString

import scala.collection.immutable

class CsvFormattingSpec extends CsvSpec {

  def documentation(): Unit = {
    // #flow-type
    import org.apache.pekko.stream.connectors.csv.scaladsl.{ CsvFormatting, CsvQuotingStyle }

    // #flow-type
    import CsvFormatting._
    val delimiter = Comma
    val quoteChar = DoubleQuote
    val escapeChar = Backslash
    val endOfLine = "\r\n"
    // format: off
    // #flow-type
    val flow: Flow[immutable.Seq[String], ByteString, _]
      = CsvFormatting.format(delimiter,
                             quoteChar,
                             escapeChar,
                             endOfLine,
                             CsvQuotingStyle.Required,
                             charset = StandardCharsets.UTF_8,
                             byteOrderMark = None)
    // #flow-type
    // format: on
    Source.single(List("a", "b")).via(flow).runWith(Sink.ignore)
  }

  "CSV Formatting" should {
    "format simple value" in assertAllStagesStopped {
      // #formatting
      import org.apache.pekko.stream.connectors.csv.scaladsl.CsvFormatting

      // #formatting
      val fut =
        // format: off
      // #formatting
      Source
        .single(List("eins", "zwei", "drei"))
        .via(CsvFormatting.format())
        .runWith(Sink.head)
      // #formatting
      // format: on
      fut.futureValue should be(ByteString("eins,zwei,drei\r\n"))
    }

    "include Byte Order Mark" in assertAllStagesStopped {
      // #formatting-bom
      import org.apache.pekko.stream.connectors.csv.scaladsl.{ ByteOrderMark, CsvFormatting }

      // #formatting-bom
      val fut =
        // format: off
      // #formatting-bom
        Source
          .apply(List(List("eins", "zwei", "drei"), List("uno", "dos", "tres")))
          .via(CsvFormatting.format(byteOrderMark = Some(ByteOrderMark.UTF_8)))
          .runWith(Sink.seq)
      // #formatting-bom
      // format: on
      fut.futureValue should be(
        List(ByteOrderMark.UTF_8, ByteString("eins,zwei,drei\r\n"), ByteString("uno,dos,tres\r\n")))
    }

  }
}
