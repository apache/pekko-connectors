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

package org.apache.pekko.stream.connectors.google.http

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import javax.net.ssl.SSLContext

class ForwardProxyHttpsContextSpec extends AnyWordSpec with Matchers {

  private def sslContext(): SSLContext = {
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, null, null)
    ctx
  }

  // the client protocols of this context are restricted: TLSv1.3 is supported but not enabled
  private def tls12OnlyContext(): SSLContext = {
    val ctx = SSLContext.getInstance("TLSv1.2")
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

    "not enable a permitted protocol that the JVM has not enabled for client connections" in {
      // a TLSv1.2 context does not enable TLSv1.3 for clients, although it supports it
      val engine =
        ForwardProxyHttpsContext.createSSLEngine(tls12OnlyContext(), Array("TLSv1.2", "TLSv1.3"), "example.com", 443)
      engine.getEnabledProtocols.toSeq shouldBe Seq("TLSv1.2")
    }

    "fail with a clear error when no permitted protocol is enabled for client connections" in {
      val ex = the[IllegalStateException] thrownBy {
        ForwardProxyHttpsContext.createSSLEngine(tls12OnlyContext(), Array("TLSv1.3"), "example.com", 443)
      }
      ex.getMessage should include("TLSv1.3")
      ex.getMessage should include("enabled: ")
    }
  }

  "ForwardProxyHttpsContext.restrictProtocols" should {

    "keep the enabled protocols that are permitted" in {
      ForwardProxyHttpsContext.restrictProtocols(Array("TLSv1.3", "TLSv1.2", "TLSv1.1"),
        Array("TLSv1.2", "TLSv1.3")).toSeq shouldBe Seq("TLSv1.3", "TLSv1.2")
    }

    "not add a permitted protocol that is not enabled" in {
      ForwardProxyHttpsContext.restrictProtocols(Array("TLSv1.2"), Array("TLSv1.2", "TLSv1.3")).toSeq shouldBe Seq(
        "TLSv1.2")
    }

    "fail when nothing remains" in {
      val ex = the[IllegalStateException] thrownBy {
        ForwardProxyHttpsContext.restrictProtocols(Array("TLSv1.2"), Array("TLSv1.3"))
      }
      ex.getMessage should include("TLSv1.3")
      ex.getMessage should include("enabled: TLSv1.2")
    }
  }

  "ForwardProxyHttpsContext.apply" should {

    "reject an unsupported TLS version" in {
      val ex = the[IllegalArgumentException] thrownBy {
        ForwardProxyHttpsContext(getClass.getResource("/cert.pem").getPath, "TLSv1.1")
      }
      ex.getMessage should include("TLSv1.1")
    }

    "build a connection context for a supported TLS version" in {
      ForwardProxyHttpsContext(getClass.getResource("/cert.pem").getPath, "TLSv1.3") should not be null
    }
  }
}
