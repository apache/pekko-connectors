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

package org.apache.pekko.stream.connectors.awsspi

import java.util.Collections

import org.apache.pekko
import pekko.http.scaladsl.model.headers.`Content-Type`
import pekko.http.scaladsl.model.MediaTypes
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PekkoHttpClientSpec extends AnyWordSpec with Matchers with OptionValues {

  "PekkoHttpClient" should {

    "parse custom content type" in {
      val contentTypeStr = "application/xml"
      val contentType = PekkoHttpClient.tryCreateCustomContentType(contentTypeStr)
      contentType.mediaType should be(MediaTypes.`application/xml`)
    }

    "remove 'ContentType' return 'ContentLength' separate from sdk headers" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Type", Collections.singletonList("application/xml"))
      headers.put("Content-Length", Collections.singletonList("123"))
      headers.put("Accept", Collections.singletonList("*/*"))

      val (contentTypeHeader, reqHeaders) = PekkoHttpClient.convertHeaders(headers)

      contentTypeHeader.value.lowercaseName() shouldBe `Content-Type`.lowercaseName
      reqHeaders should have size 1
    }
  }
}
