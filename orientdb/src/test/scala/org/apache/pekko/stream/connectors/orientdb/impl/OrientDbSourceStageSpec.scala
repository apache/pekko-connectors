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

package org.apache.pekko.stream.connectors.orientdb.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OrientDbSourceStageSpec extends AnyWordSpec with Matchers {

  "OrientDbSourceStage.validateClassName" should {

    "accept valid class names" in {
      OrientDbSourceStage.validateClassName("User")
      OrientDbSourceStage.validateClassName("MyClass")
      OrientDbSourceStage.validateClassName("_internal")
      OrientDbSourceStage.validateClassName("Class123")
      OrientDbSourceStage.validateClassName("my_class")
    }

    "reject null class name" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName(null)
      }
    }

    "reject empty class name" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName("")
      }
    }

    "reject class name with SQL injection attempt" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName("User; DROP TABLE users; --")
      }
    }

    "reject class name with quotes" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName("User\" OR 1=1 --")
      }
    }

    "reject class name starting with digit" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName("123Class")
      }
    }

    "reject class name with spaces" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName("My Class")
      }
    }

    "reject class name with special characters" in {
      assertThrows[IllegalArgumentException] {
        OrientDbSourceStage.validateClassName("Class@name")
      }
    }
  }
}
