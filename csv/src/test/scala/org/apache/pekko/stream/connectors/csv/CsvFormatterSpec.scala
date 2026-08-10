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

package org.apache.pekko.stream.connectors.csv

import java.nio.charset.StandardCharsets

import org.apache.pekko
import pekko.stream.connectors.csv.impl.CsvFormatter
import pekko.stream.connectors.csv.scaladsl.CsvQuotingStyle
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CsvFormatterSpec extends AnyWordSpec with Matchers with LogCapturing {

  "CSV Formatter comma as delimiter" should {
    val formatter = new CsvFormatter(',', '\"', '\\', "\r\n", CsvQuotingStyle.Required)

    "format Strings" in {
      expectInOut(formatter, "ett", "två", "tre")("ett,två,tre\r\n")
    }

    "format Strings containing commas" in {
      expectInOut(formatter, "ett", "t,vå", "tre")("ett,\"t,vå\",tre\r\n")
    }

    "format Strings containing quotes" in {
      expectInOut(formatter, "ett", "t\"vå", "tre")("ett,\"t\"\"vå\",tre\r\n")
    }

  }

  "CSV Formatter quoting everything" should {
    val formatter = new CsvFormatter(',', '\"', '\\', "\r\n", CsvQuotingStyle.Always)

    "format Strings" in {
      expectInOut(formatter, "ett", "två", "tre")(""""ett","två","tre"""" + "\r\n")
    }

    "format Strings with commas" in {
      expectInOut(formatter, "ett", "t,vå", "tre")(""""ett","t,vå","tre"""" + "\r\n")
    }

    "format Strings containing quotes" in {
      expectInOut(formatter, "ett", "t\"vå", "tre")(""""ett","t""vå","tre"""" + "\r\n")
    }

    "format Strings containing quotes twice" in {
      expectInOut(formatter, "ett", "t\"v\"å", "tre")(""""ett","t""v""å","tre"""" + "\r\n")
    }

  }

  "CSV Formatter with required quoting" should {
    val formatter = new CsvFormatter(';', '\"', '\\', "\r\n", CsvQuotingStyle.Required)

    "format Strings" in {
      expectInOut(formatter, "ett", "två", "tre")("ett;två;tre\r\n")
    }

    "quote Strings with delimiters" in {
      expectInOut(formatter, "ett", "t;vå", "tre")("ett;\"t;vå\";tre\r\n")
    }

    "quote Strings with quotes" in {
      expectInOut(formatter, "ett", "t\"vå", "tre")("""ett;"t""vå";tre""" + "\r\n")
    }

    "quote Strings with quote at end" in {
      expectInOut(formatter, "ett", "två\"", "tre")("ett;\"två\"\"\";tre\r\n")
    }

    "quote Strings with just a quote" in {
      expectInOut(formatter, "ett", "\"", "tre")("ett;\"\"\"\";tre\r\n")
    }

    "quote Strings containing LF" in {
      expectInOut(formatter, "ett", "\n", "tre")("ett;\"\n\";tre\r\n")
    }

    "quote Strings containing CR, LF" in {
      expectInOut(formatter, "ett", "prefix\r\npostfix", "tre")("ett;\"prefix\r\npostfix\";tre\r\n")
    }

    "duplicate escape char" in {
      expectInOut(formatter, "ett", "prefix\\postfix", "tre")("ett;\"prefix\\\\postfix\";tre\r\n")
    }

    "duplicate escape chars and quotes" in {
      expectInOut(formatter, "ett", "one\\two\"three\\four", "tre")("ett;\"one\\\\two\"\"three\\\\four\";tre\r\n")
    }
  }

  "CSV Formatter with non-standard charset" should {
    val charset = StandardCharsets.UTF_16LE
    val formatter = new CsvFormatter(';', '\"', '\\', "\r\n", CsvQuotingStyle.Required, charset)

    "get the encoding right" in {
      val csv = formatter.toCsv(List("ett", "två", "อักษรไทย"))
      val arr1 = new Array[Byte](csv.length)
      csv.copyToArray(arr1)
      new String(arr1, charset) should be("ett;två;อักษรไทย\r\n")
    }
  }

  "CSV Formatter with formula injection mitigation" should {
    val formatter =
      new CsvFormatter(',', '"', '\\', "\r\n", CsvQuotingStyle.Required, StandardCharsets.UTF_8, true)

    "prefix formula starting with =" in {
      expectInOut(formatter, "=SUM(A1)")("\"'=SUM(A1)\"\r\n")
    }

    "prefix formula starting with +" in {
      expectInOut(formatter, "+cmd|' /C calc'!A0")("\"'+cmd|' /C calc'!A0\"\r\n")
    }

    "prefix formula starting with -" in {
      expectInOut(formatter, "-2+3")("\"'-2+3\"\r\n")
    }

    "prefix formula starting with @" in {
      expectInOut(formatter, "@SUM(A1)")("\"'@SUM(A1)\"\r\n")
    }

    "not prefix normal text" in {
      expectInOut(formatter, "normal", "text")("normal,text\r\n")
    }

    "not prefix empty string" in {
      expectInOut(formatter, "", "text")(",text\r\n")
    }

    "handle mixed fields" in {
      expectInOut(formatter, "safe", "=DANGEROUS", "also-safe")("safe,\"'=DANGEROUS\",also-safe\r\n")
    }
  }

  private def expectInOut(formatter: CsvFormatter, in: String*)(expect: String): Unit =
    formatter.toCsv(in.toList).utf8String should be(expect)

}
