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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ElasticsearchSearchBodySpec extends AnyWordSpec with Matchers {

  "ElasticsearchSourceStage.buildSearchBody" should {

    "produce valid JSON for simple keys" in {
      val body = ElasticsearchSourceStage.buildSearchBody(
        Map("query" -> """{"match_all":{}}""", "size" -> "10"))
      body should include(""""query":{"match_all":{}}""")
      body should include(""""size":10""")
      body should startWith("{")
      body should endWith("}")
    }

    "escape double quotes in keys" in {
      val body = ElasticsearchSourceStage.buildSearchBody(
        Map("key\"injected" -> "true"))
      body shouldBe """{"key\"injected":true}"""
    }

    "escape backslashes in keys" in {
      val body = ElasticsearchSourceStage.buildSearchBody(
        Map("key\\name" -> "true"))
      body shouldBe """{"key\\name":true}"""
    }

    "escape newlines in keys" in {
      val body = ElasticsearchSourceStage.buildSearchBody(
        Map("key\nname" -> "true"))
      body shouldBe """{"key\nname":true}"""
    }

    "handle empty map" in {
      ElasticsearchSourceStage.buildSearchBody(Map.empty) shouldBe "{}"
    }

    "handle multiple entries" in {
      val body = ElasticsearchSourceStage.buildSearchBody(
        Map("a" -> "1", "b" -> "2"))
      body should include(""""a":1""")
      body should include(""""b":2""")
    }
  }
}
