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

package org.apache.pekko.stream.connectors.pravega.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ListBuffer

class PravegaTableCleanupSpec extends AnyWordSpec with Matchers {

  "PravegaTableCleanup.closeTableAndFactory" should {

    "close the table and then the factory" in {
      val closed = ListBuffer.empty[String]
      PravegaTableCleanup.closeTableAndFactory(closed += "table", closed += "factory")((_, _) =>
        fail("no error expected"))
      closed.toList shouldBe List("table", "factory")
    }

    "still close the factory when closing the table fails" in {
      // the factory owns the connection pool, so it must be released even
      // when the table close throws
      val closed = ListBuffer.empty[String]
      val errors = ListBuffer.empty[String]
      PravegaTableCleanup.closeTableAndFactory(
        throw new RuntimeException("table boom"),
        closed += "factory")((what, _) => errors += what)

      closed.toList shouldBe List("factory")
      errors.toList shouldBe List("table")
    }

    "close the factory when the table was never opened" in {
      // preStart creates the factory first: if forKeyValueTable throws, the
      // table field is still null and only the factory needs closing
      val table: AutoCloseable = null
      val closed = ListBuffer.empty[String]
      noException should be thrownBy {
        PravegaTableCleanup.closeTableAndFactory(
          if (table ne null) table.close(),
          closed += "factory")((_, _) => fail("no error expected"))
      }
      closed.toList shouldBe List("factory")
    }

    "report both failures and not rethrow" in {
      val errors = ListBuffer.empty[String]
      noException should be thrownBy {
        PravegaTableCleanup.closeTableAndFactory(
          throw new RuntimeException("a"),
          throw new RuntimeException("b"))((what, _) => errors += what)
      }
      errors.toList shouldBe List("table", "key value table factory")
    }
  }
}
