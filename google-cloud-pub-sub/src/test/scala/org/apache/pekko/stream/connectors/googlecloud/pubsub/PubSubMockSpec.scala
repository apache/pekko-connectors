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

package org.apache.pekko.stream.connectors.googlecloud.pubsub

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.{ ConnectionContext, Http }
import pekko.stream.connectors.googlecloud.pubsub.impl.{ NoopTrustManager, PubSubApi }
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

import javax.net.ssl.{ SSLContext, SSLEngine }
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait PubSubMockSpec extends Suite with BeforeAndAfterAll {
  implicit val system: ActorSystem

  def createInsecureSslEngine(host: String, port: Int): SSLEngine = {
    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array(new NoopTrustManager()), null)

    val engine = sslContext.createSSLEngine(host, port)
    engine.setUseClientMode(true)

    engine
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Http().setDefaultClientHttpsContext(ConnectionContext.httpsClient(createInsecureSslEngine _))
  }

  val wiremockServer = new WireMockServer(
    wireMockConfig().dynamicPort().dynamicHttpsPort().notifier(new ConsoleNotifier(false)))
  wiremockServer.start()

  val wireMock = new WireMock("localhost", wiremockServer.port())

  object TestHttpApi extends PubSubApi {
    val isEmulated = false
    val PubSubGoogleApisHost = "localhost"
    val PubSubGoogleApisPort = wiremockServer.httpsPort()
  }

  object TestEmulatorHttpApi extends PubSubApi {
    override val isEmulated = true
    val PubSubGoogleApisHost = "localhost"
    val PubSubGoogleApisPort = wiremockServer.port()
  }

  val config = PubSubConfig()

}
