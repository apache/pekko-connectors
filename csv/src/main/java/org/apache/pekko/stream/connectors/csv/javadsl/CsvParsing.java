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

package org.apache.pekko.stream.connectors.csv.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;
import scala.collection.JavaConverters;

import java.util.Collection;

public class CsvParsing {

  public static final byte BACKSLASH = '\\';
  public static final byte COMMA = ',';
  public static final byte SEMI_COLON = ';';
  public static final byte COLON = ':';
  public static final byte TAB = '\t';
  public static final byte DOUBLE_QUOTE = '"';
  public static final int MAXIMUM_LINE_LENGTH_DEFAULT = 10 * 1024;

  public static Flow<ByteString, Collection<ByteString>, NotUsed> lineScanner() {
    return lineScanner(COMMA, DOUBLE_QUOTE, BACKSLASH, MAXIMUM_LINE_LENGTH_DEFAULT);
  }

  public static Flow<ByteString, Collection<ByteString>, NotUsed> lineScanner(
      byte delimiter, byte quoteChar, byte escapeChar) {
    return lineScanner(delimiter, quoteChar, escapeChar, MAXIMUM_LINE_LENGTH_DEFAULT);
  }

  public static Flow<ByteString, Collection<ByteString>, NotUsed> lineScanner(
      byte delimiter, byte quoteChar, byte escapeChar, int maximumLineLength) {
    return org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing.lineScanner(
            delimiter, quoteChar, escapeChar, maximumLineLength)
        .asJava()
        .map(c -> JavaConverters.asJavaCollectionConverter(c).asJavaCollection())
        .mapMaterializedValue(m -> NotUsed.getInstance());
  }
}
