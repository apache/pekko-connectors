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

package org.apache.pekko.stream.connectors.s3

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class S3ExceptionSpec extends AnyFlatSpecLike with Matchers {

  "S3 exception" should "be parsed" in {
    val e = S3Exception("Hej", StatusCodes.OK)
    e.toString shouldBe "org.apache.pekko.stream.connectors.s3.S3Exception: Hej (Status code: 200 OK, Code: 200 OK, RequestId: -, Resource: -)"
  }

  it should "parse AWS sample" in {
    val s = """<?xml version="1.0" encoding="UTF-8"?>
              |<Error>
              |  <Code>NoSuchKey</Code>
              |  <Message>The resource you requested does not exist</Message>
              |  <Resource>/mybucket/myfoto.jpg</Resource>
              |  <RequestId>4442587FB7D0A2F9</RequestId>
              |</Error>""".stripMargin
    val e = S3Exception(s, StatusCodes.NotFound)
    e.code shouldBe "NoSuchKey"
    e.message shouldBe "The resource you requested does not exist"
    e.requestId shouldBe "4442587FB7D0A2F9"
    e.resource shouldBe "/mybucket/myfoto.jpg"
    e.toString shouldBe "org.apache.pekko.stream.connectors.s3.S3Exception: The resource you requested does not exist (Status code: 404 Not Found, Code: NoSuchKey, RequestId: 4442587FB7D0A2F9, Resource: /mybucket/myfoto.jpg)"
  }

  it should "survive null" in {
    val e = S3Exception(null, StatusCodes.NotFound)
    e.toString shouldBe "org.apache.pekko.stream.connectors.s3.S3Exception (Status code: 404 Not Found, Code: 404 Not Found, RequestId: -, Resource: -)"
  }

}
