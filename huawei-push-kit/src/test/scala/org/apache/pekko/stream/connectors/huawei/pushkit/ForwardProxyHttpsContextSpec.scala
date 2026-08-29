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

package org.apache.pekko.stream.connectors.huawei.pushkit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import javax.net.ssl.SSLContext

class ForwardProxyHttpsContextSpec extends AnyWordSpec with Matchers {

  private def sslContext(): SSLContext = {
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, null, null)
    ctx
  }

  "ForwardProxyHttpsContext.createSSLEngine" should {

    "enable only the requested protocols" in {
      val engine =
        ForwardProxyHttpsContext.createSSLEngine(sslContext(), Array("TLSv1.3"), "example.com", 443)
      engine.getEnabledProtocols.toSeq shouldBe Seq("TLSv1.3")
    }

    "enable TLSv1.2 and TLSv1.3 when TLSv1.2 is the minimum" in {
      val engine =
        ForwardProxyHttpsContext.createSSLEngine(sslContext(), Array("TLSv1.2", "TLSv1.3"), "example.com", 443)
      engine.getEnabledProtocols.toSet shouldBe Set("TLSv1.2", "TLSv1.3")
    }

    "never enable protocols older than TLSv1.2" in {
      val engine =
        ForwardProxyHttpsContext.createSSLEngine(sslContext(), Array("TLSv1.2", "TLSv1.3"), "example.com", 443)
      engine.getEnabledProtocols should contain noneOf ("SSLv3", "TLSv1", "TLSv1.1")
    }

    "use client mode" in {
      val engine =
        ForwardProxyHttpsContext.createSSLEngine(sslContext(), Array("TLSv1.2", "TLSv1.3"), "example.com", 443)
      engine.getUseClientMode shouldBe true
    }

    "retain https endpoint identification so hostname verification stays enabled" in {
      val engine =
        ForwardProxyHttpsContext.createSSLEngine(sslContext(), Array("TLSv1.2", "TLSv1.3"), "example.com", 443)
      engine.getSSLParameters.getEndpointIdentificationAlgorithm shouldBe "https"
    }
  }
}
