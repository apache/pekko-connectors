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

  private def closeable(body: => Unit): AutoCloseable = new AutoCloseable {
    override def close(): Unit = body
  }

  "PravegaTableCleanup.closeTableAndFactory" should {

    "close the table and then the factory" in {
      val closed = ListBuffer.empty[String]
      val result =
        PravegaTableCleanup.closeTableAndFactory(closeable(closed += "table"), closeable(closed += "factory"))((_, _) =>
          fail("no error expected"))
      closed.toList shouldBe List("table", "factory")
      result shouldBe true
    }

    "still close the factory when closing the table fails" in {
      // the factory owns the connection pool, so it must be released even
      // when the table close throws
      val closed = ListBuffer.empty[String]
      val errors = ListBuffer.empty[String]
      val result = PravegaTableCleanup.closeTableAndFactory(
        closeable(throw new RuntimeException("table boom")),
        closeable(closed += "factory"))((what, _) => errors += what)

      closed.toList shouldBe List("factory")
      errors.toList shouldBe List("table")
      result shouldBe false
    }

    "still close the factory when closing the table fails with an error that is not NonFatal" in {
      // e.g. the stage is stopped on an interrupted thread
      val closed = ListBuffer.empty[String]
      val errors = ListBuffer.empty[String]
      an[InterruptedException] should be thrownBy {
        PravegaTableCleanup.closeTableAndFactory(
          closeable(throw new InterruptedException("table interrupted")),
          closeable(closed += "factory"))((what, _) => errors += what)
      }

      closed.toList shouldBe List("factory")
      errors.toList shouldBe empty
    }

    "still close the factory when reporting the table failure throws" in {
      val closed = ListBuffer.empty[String]
      a[IllegalStateException] should be thrownBy {
        PravegaTableCleanup.closeTableAndFactory(
          closeable(throw new RuntimeException("table boom")),
          closeable(closed += "factory"))((what, _) =>
          if (what == "table") throw new IllegalStateException("reporting failed"))
      }

      closed.toList shouldBe List("factory")
    }

    "close the factory when the table was never opened" in {
      // preStart creates the factory first: if forKeyValueTable throws, the
      // table field is still null and only the factory needs closing
      val closed = ListBuffer.empty[String]
      val result = PravegaTableCleanup.closeTableAndFactory(null, closeable(closed += "factory"))((_, _) =>
        fail("no error expected"))
      closed.toList shouldBe List("factory")
      result shouldBe true
    }

    "do nothing when neither the factory nor the table was opened" in {
      PravegaTableCleanup.closeTableAndFactory(null, null)((_, _) => fail("no error expected")) shouldBe true
    }

    "report both failures and not rethrow" in {
      val errors = ListBuffer.empty[String]
      val result = PravegaTableCleanup.closeTableAndFactory(
        closeable(throw new RuntimeException("a")),
        closeable(throw new RuntimeException("b")))((what, _) => errors += what)
      errors.toList shouldBe List("table", "key value table factory")
      result shouldBe false
    }
  }
}
