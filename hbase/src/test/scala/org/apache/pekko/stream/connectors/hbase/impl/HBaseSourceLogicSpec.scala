/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.hbase.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ListBuffer

class HBaseSourceLogicSpec extends AnyWordSpec with Matchers {

  "HBaseSourceLogic.closeAll" should {

    "close the scanner, the table and the connection" in {
      val closed = ListBuffer.empty[String]
      HBaseSourceLogic.closeAll(closed += "scanner", closed += "table", closed += "connection")((_, _) =>
        fail("no error expected"))
      closed.toList shouldBe List("scanner", "table", "connection")
    }

    "still close the table and the connection when the scanner fails to close" in {
      val closed = ListBuffer.empty[String]
      val errors = ListBuffer.empty[String]
      HBaseSourceLogic.closeAll(
        throw new RuntimeException("scanner boom"),
        closed += "table",
        closed += "connection")((what, _) => errors += what)

      // the connection is the expensive resource: it must be released even
      // when an earlier close throws
      closed.toList shouldBe List("table", "connection")
      errors.toList shouldBe List("scanner")
    }

    "still close the connection when the table fails to close" in {
      val closed = ListBuffer.empty[String]
      val errors = ListBuffer.empty[String]
      HBaseSourceLogic.closeAll(closed += "scanner", throw new RuntimeException("table boom"),
        closed += "connection")((what, _) => errors += what)

      closed.toList shouldBe List("scanner", "connection")
      errors.toList shouldBe List("table")
    }

    "report every failure and not rethrow" in {
      val errors = ListBuffer.empty[String]
      noException should be thrownBy {
        HBaseSourceLogic.closeAll(
          throw new RuntimeException("a"),
          throw new RuntimeException("b"),
          throw new RuntimeException("c"))((what, _) => errors += what)
      }
      errors.toList shouldBe List("scanner", "table", "connection")
    }
  }
}
