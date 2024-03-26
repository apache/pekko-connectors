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

class MalformedCsvException private[csv] (val lineNo: Long, val bytePos: Int, msg: String) extends Exception(msg) {

  /**
   * Java API:
   * Returns the line number where the parser failed.
   */
  def getLineNo: Long = lineNo

  /**
   * Java API:
   * Returns the byte within the parsed line where the parser failed.
   */
  def getBytePos: Int = bytePos
}
