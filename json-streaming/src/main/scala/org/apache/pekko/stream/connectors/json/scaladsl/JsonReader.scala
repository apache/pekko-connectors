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

package org.apache.pekko.stream.connectors.json.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.json.impl.JsonStreamReader
import pekko.stream.scaladsl.Flow
import pekko.util.ByteString
import org.jsfr.json.compiler.JsonPathCompiler
import org.jsfr.json.path.JsonPath

object JsonReader {

  /**
   * A Flow that consumes incoming json in chunks and produces a stream of parsable json values
   * according to the JsonPath given.
   *
   * JsonPath examples:
   * - Stream all elements of the nested array `rows`: `$.rows[*]`
   * - Stream the value of `doc` of each element in the array: `$.rows[*].doc`
   *
   * Supported JsonPath syntax: https://github.com/jsurfer/JsonSurfer#what-is-jsonpath
   */
  def select(path: JsonPath): Flow[ByteString, ByteString, NotUsed] = Flow.fromGraph(new JsonStreamReader(path))

  /**
   * A Flow that consumes incoming json in chunks and produces a stream of parsable json values
   * according to the JsonPath given. The passed String will need to be parsed first.
   *
   * @see [[#select]]
   */
  def select(path: String): Flow[ByteString, ByteString, NotUsed] = select(JsonPathCompiler.compile(path))
}
