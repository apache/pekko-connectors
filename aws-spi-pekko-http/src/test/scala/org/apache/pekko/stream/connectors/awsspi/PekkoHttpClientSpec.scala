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
import java.nio.ByteBuffer
import com.typesafe.config.ConfigFactory

import org.apache.pekko
import pekko.http.scaladsl.model.headers.`Content-Type`
import pekko.http.scaladsl.model.{ ContentTypes, HttpMethods, MediaTypes }
import pekko.http.scaladsl.settings.{ ClientConnectionSettings, ConnectionPoolSettings }
import org.reactivestreams.Subscriber
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.async.SdkHttpContentPublisher
import software.amazon.awssdk.utils.AttributeMap

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

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

      val (contentTypeHeader, reqHeaders, contentLength) = PekkoHttpClient.convertHeaders(headers)

      contentTypeHeader.value.lowercaseName() shouldBe `Content-Type`.lowercaseName
      reqHeaders should have size 1
      contentLength shouldBe Some(123L)
    }

    "return None content length when Content-Length header is absent" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Type", Collections.singletonList("application/xml"))
      headers.put("Accept", Collections.singletonList("*/*"))

      val (_, _, contentLength) = PekkoHttpClient.convertHeaders(headers)

      contentLength shouldBe None
    }
    "use sdk content length from headers when publisher returns empty contentLength" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.empty()
        override def subscribe(s: Subscriber[_ >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.PUT, ContentTypes.NoContentType, publisher,
          Some(42L))
      entity.contentLengthOption shouldBe Some(42L)
    }

    "use publisher contentLength when sdkContentLength is absent" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(99L)
        override def subscribe(s: Subscriber[_ >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.PUT, ContentTypes.NoContentType, publisher, None)
      entity.contentLengthOption shouldBe Some(99L)
    }

    "prefer sdk content length over publisher contentLength when both are present" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(55L)
        override def subscribe(s: Subscriber[_ >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.PUT, ContentTypes.NoContentType, publisher,
          Some(42L))
      entity.contentLengthOption shouldBe Some(42L)
    }

    "build() should use default ConnectionPoolSettings" in {
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .build()
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings shouldBe ConnectionPoolSettings(ConfigFactory.load())
    }

    "withConnectionPoolSettingsBuilderFromAttributeMap().buildWithDefaults() should propagate configuration options" in {
      val attributeMap = AttributeMap.builder()
        .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, 1.second.toJava)
        .put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, 2.second.toJava)
        .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, Integer.valueOf(3))
        .put(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE, 4.second.toJava)
        .build()
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .withConnectionPoolSettingsBuilderFromAttributeMap()
        .buildWithDefaults(attributeMap)
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings.connectionSettings.connectingTimeout shouldBe 1.second
      pekkoClient.connectionSettings.connectionSettings.idleTimeout shouldBe 2.seconds
      pekkoClient.connectionSettings.maxConnections shouldBe 3
      pekkoClient.connectionSettings.maxConnectionLifetime shouldBe 4.seconds
    }

    "withConnectionPoolSettingsBuilderFromAttributeMap().build() should fallback to GLOBAL_HTTP_DEFAULTS" in {
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .withConnectionPoolSettingsBuilderFromAttributeMap()
        .build()
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings.connectionSettings.connectingTimeout shouldBe
      SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toScala
      pekkoClient.connectionSettings.connectionSettings.idleTimeout shouldBe
      SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(
        SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toScala
      pekkoClient.connectionSettings.maxConnections shouldBe
      SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.MAX_CONNECTIONS).intValue()
      infiniteToZero(pekkoClient.connectionSettings.maxConnectionLifetime) shouldBe
      SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE)
    }

    "withConnectionPoolSettingsBuilder().build() should use passed connectionPoolSettings builder" in {
      val connectionPoolSettings = ConnectionPoolSettings(ConfigFactory.load())
        .withConnectionSettings(
          ClientConnectionSettings(ConfigFactory.load())
            .withConnectingTimeout(1.second)
            .withIdleTimeout(2.seconds)
        )
        .withMaxConnections(3)
        .withMaxConnectionLifetime(4.seconds)

      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .withConnectionPoolSettingsBuilder((_, _) => connectionPoolSettings)
        .build()
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings shouldBe connectionPoolSettings
    }

    "withConnectionPoolSettings().build() should use passed ConnectionPoolSettings" in {
      val connectionPoolSettings = ConnectionPoolSettings(ConfigFactory.load())
        .withConnectionSettings(
          ClientConnectionSettings(ConfigFactory.load())
            .withConnectingTimeout(1.second)
            .withIdleTimeout(2.seconds)
        )
        .withMaxConnections(3)
        .withMaxConnectionLifetime(4.seconds)
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .withConnectionPoolSettings(connectionPoolSettings)
        .build()
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings shouldBe connectionPoolSettings
    }
  }

  private def infiniteToZero(duration: scala.concurrent.duration.Duration): java.time.Duration = duration match {
    case duration: FiniteDuration => duration.toJava
    case _                        => java.time.Duration.ZERO
  }
}
