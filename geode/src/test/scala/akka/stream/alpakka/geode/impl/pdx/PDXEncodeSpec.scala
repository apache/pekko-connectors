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

package akka.stream.alpakka.geode.impl.pdx

import java.util.{ Date, UUID }

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PDXEncodeSpec extends AnyWordSpec with Matchers {

  "PDXEncoder" should {

    "provides encoder for primitive types" in {
      PdxEncoder[Boolean]
      PdxEncoder[Int]
      PdxEncoder[List[Int]]
      PdxEncoder[Array[Int]]
      PdxEncoder[Double]
      PdxEncoder[List[Double]]
      PdxEncoder[Array[Double]]
      PdxEncoder[Float]
      PdxEncoder[List[Float]]
      PdxEncoder[Array[Float]]
      PdxEncoder[Long]
      PdxEncoder[Char]
      PdxEncoder[String]

    }

    "provides encoder for basic types" in {
      PdxEncoder[Date]
      PdxEncoder[UUID]
    }
  }
}
