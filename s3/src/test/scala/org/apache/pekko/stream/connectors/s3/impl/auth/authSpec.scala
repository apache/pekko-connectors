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

package org.apache.pekko.stream.connectors.s3.impl.auth

import org.scalatest.flatspec.AnyFlatSpec

class authSpec extends AnyFlatSpec {

  "encodeHex" should "encode string to hex string" in {
    assert(encodeHex("1234+abcd".getBytes()) == "313233342b61626364")
  }

}
