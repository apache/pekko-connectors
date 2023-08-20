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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json.{ JsonParser, ParserInput }

class BigQueryJsonProtocolSpec extends BigQueryJsonProtocol with AnyWordSpecLike with Matchers {

  val json = """{
               |  "f": [
               |    {
               |      "v": "Peter"
               |    },
               |    {
               |      "v": [
               |        {
               |          "v": {
               |            "f": [
               |              {
               |                "v": "street1"
               |              },
               |              {
               |                "v": "city1"
               |              }
               |            ]
               |          }
               |        },
               |        {
               |          "v": {
               |            "f": [
               |              {
               |                "v": "street2"
               |              },
               |              {
               |                "v": "city2"
               |              }
               |            ]
               |          }
               |        }
               |      ]
               |    }
               |  ]
               |}
               |""".stripMargin

  case class Record(name: Option[String], addresses: Seq[Address])
  case class Address(street: Option[String], city: Option[String])

  implicit val addressFormat: BigQueryRootJsonFormat[Address] = bigQueryJsonFormat2(Address.apply)
  implicit val recordFormat: BigQueryRootJsonFormat[Record] = bigQueryJsonFormat2(Record.apply)

  "BigQueryJsonProtocol" should {

    "parse nested case classes" in {
      recordFormat.read(JsonParser(ParserInput(json))) shouldEqual Record(Some("Peter"),
        Seq(Address(Some("street1"), Some("city1")),
          Address(Some("street2"), Some("city2"))))
    }

  }
}
