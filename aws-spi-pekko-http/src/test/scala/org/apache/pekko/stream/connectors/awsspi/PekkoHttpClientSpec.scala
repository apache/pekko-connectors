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
import ch.qos.logback.classic.{ Level, Logger => LogbackLogger }
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import javax.net.ssl.SSLContext
import com.typesafe.config.ConfigFactory

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.ConnectionContext
import pekko.http.scaladsl.model.headers.`Content-Type`
import pekko.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpHeader, HttpMethods, MediaTypes }
import pekko.http.scaladsl.settings.{ ClientConnectionSettings, ConnectionPoolSettings }
import org.reactivestreams.Subscriber
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.async.SdkHttpContentPublisher
import software.amazon.awssdk.utils.AttributeMap

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._

class PekkoHttpClientSpec extends AnyWordSpec with Matchers with OptionValues {

  "PekkoHttpClient" should {

    "parse custom content type" in {
      val contentTypeStr = "application/xml"
      val contentType = PekkoHttpClient.tryCreateCustomContentType(contentTypeStr)
      contentType.mediaType should be(MediaTypes.`application/xml`)
    }

    "log the content type it parses with the argument substituted into the message" in {
      val contentTypeStr = "application/xml"
      val logbackLogger = PekkoHttpClient.logger.asInstanceOf[LogbackLogger]
      val appender = new ListAppender[ILoggingEvent]
      val previousLevel = logbackLogger.getLevel
      appender.start()
      logbackLogger.addAppender(appender)
      logbackLogger.setLevel(Level.DEBUG)
      try {
        PekkoHttpClient.tryCreateCustomContentType(contentTypeStr)
      } finally {
        logbackLogger.setLevel(previousLevel)
        logbackLogger.detachAppender(appender)
        appender.stop()
      }

      val messages = appender.list.asScala.map(_.getFormattedMessage).toList
      messages should contain(s"Try to parse content type from $contentTypeStr")
    }

    "parse custom content type with parameters preserving the parameters" in {
      val contentType = PekkoHttpClient.tryCreateCustomContentType("text/plain; charset=UTF-8")
      contentType.mediaType.mainType shouldBe "text"
      contentType.mediaType.subType shouldBe "plain"
      contentType.charsetOption.map(_.value) shouldBe Some("UTF-8")
    }

    "fall back to a custom binary content type when proper parsing fails" in {
      val contentType = PekkoHttpClient.tryCreateCustomContentType("foo/bar baz")
      contentType.mediaType.mainType shouldBe "foo"
      contentType.mediaType.subType shouldBe "bar baz"
    }

    "map form-urlencoded content type with charset to the predefined content type" in {
      val header = HttpHeader.parse("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8") match {
        case HttpHeader.ParsingResult.Ok(h, _) => h
        case error                             => fail(s"could not parse header: $error")
      }
      val contentType = PekkoHttpClient.contentTypeHeaderToContentType(Some(header))
      contentType.mediaType.subType shouldBe "x-www-form-urlencoded"
      contentType shouldBe PekkoHttpClient.formUrlEncoded
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

    "convert a multi-value header to one header per value" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Accept-Encoding", java.util.Arrays.asList("gzip", "deflate"))

      val (contentTypeHeader, reqHeaders, contentLength) = PekkoHttpClient.convertHeaders(headers)

      contentTypeHeader shouldBe None
      contentLength shouldBe None
      reqHeaders.map(h => (h.name(), h.value())) should contain theSameElementsInOrderAs
      Seq(("Accept-Encoding", "gzip"), ("Accept-Encoding", "deflate"))
    }

    "preserve the order of the values of each header" in {
      val headers = new java.util.LinkedHashMap[String, java.util.List[String]]
      headers.put("X-First", java.util.Arrays.asList("a", "b", "c"))
      headers.put("X-Second", java.util.Arrays.asList("d"))
      headers.put("X-Third", java.util.Arrays.asList("e", "f"))

      val (_, reqHeaders, _) = PekkoHttpClient.convertHeaders(headers)

      // convertHeaders does not guarantee the relative order of different header names,
      // but the values of each header must stay in the order the SDK supplied them
      reqHeaders.groupBy(_.name()).map { case (name, hs) => name -> hs.map(_.value()) } shouldBe Map(
        "X-First" -> Seq("a", "b", "c"),
        "X-Second" -> Seq("d"),
        "X-Third" -> Seq("e", "f"))
    }

    "reject a Content-Length header with multiple values" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Length", java.util.Arrays.asList("123", "456"))

      an[IllegalArgumentException] should be thrownBy PekkoHttpClient.convertHeaders(headers)
    }

    "reject a Content-Type header with multiple values" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Type", java.util.Arrays.asList("application/xml", "application/json"))

      an[IllegalArgumentException] should be thrownBy PekkoHttpClient.convertHeaders(headers)
    }

    "reject a header with no values" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Accept", Collections.emptyList[String]())

      an[IllegalArgumentException] should be thrownBy PekkoHttpClient.convertHeaders(headers)
    }

    "reject a non-numeric Content-Length header value with a clear error" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Length", Collections.singletonList("not-a-number"))

      val e = the[IllegalArgumentException] thrownBy PekkoHttpClient.convertHeaders(headers)
      e.getMessage should include("not-a-number")
    }

    "reject Content-Length header values that are not of the RFC 9110 1*DIGIT form" in {
      // all of these are accepted by String.toLong, apart from the empty and blank values
      Seq("+5", "-0", "\u0661\u0662", "\uff11\uff12", " 5", "5 ", "", " ").foreach { value =>
        val headers = new java.util.HashMap[String, java.util.List[String]]
        headers.put("Content-Length", Collections.singletonList(value))

        withClue(s"Content-Length '$value': ") {
          val e = the[IllegalArgumentException] thrownBy PekkoHttpClient.convertHeaders(headers)
          e.getMessage should include("Content-Length")
        }
      }
    }

    "accept Content-Length header values of the 1*DIGIT form, including leading zeros" in {
      Seq("0" -> 0L, "42" -> 42L, "007" -> 7L, Long.MaxValue.toString -> Long.MaxValue).foreach {
        case (value, expected) =>
          val headers = new java.util.HashMap[String, java.util.List[String]]
          headers.put("Content-Length", Collections.singletonList(value))

          PekkoHttpClient.convertHeaders(headers)._3 shouldBe Some(expected)
      }
    }

    "reject a Content-Length header value that overflows a Long with a clear error" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Length", Collections.singletonList("9223372036854775808"))

      val e = the[IllegalArgumentException] thrownBy PekkoHttpClient.convertHeaders(headers)
      e.getMessage should include("9223372036854775808")
    }

    "reject a negative Content-Length header value" in {
      val headers = new java.util.HashMap[String, java.util.List[String]]
      headers.put("Content-Length", Collections.singletonList("-1"))

      an[IllegalArgumentException] should be thrownBy PekkoHttpClient.convertHeaders(headers)
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
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.PUT, ContentTypes.NoContentType, publisher,
          Some(42L))
      entity.contentLengthOption shouldBe Some(42L)
    }

    "use publisher contentLength when sdkContentLength is absent" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(99L)
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.PUT, ContentTypes.NoContentType, publisher, None)
      entity.contentLengthOption shouldBe Some(99L)
    }

    "prefer sdk content length over publisher contentLength when both are present" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(55L)
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.PUT, ContentTypes.NoContentType, publisher,
          Some(42L))
      entity.contentLengthOption shouldBe Some(42L)
    }

    "attach the request body for a DELETE request when the SDK provides content" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(10L)
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.DELETE, ContentTypes.`application/json`, publisher)
      entity.contentLengthOption shouldBe Some(10L)
      entity.contentType shouldBe ContentTypes.`application/json`
    }

    "attach the request body for a GET request when the SDK headers provide a content length" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.empty()
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      val entity =
        PekkoHttpClient.entityForMethodAndContentType(HttpMethods.GET, ContentTypes.NoContentType, publisher,
          Some(5L))
      entity.contentLengthOption shouldBe Some(5L)
    }

    "keep a GET request bodiless when the declared content length is zero" in {
      val emptyPublisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(0L)
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      val unknownLengthPublisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.empty()
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      PekkoHttpClient.entityForMethodAndContentType(HttpMethods.GET, ContentTypes.NoContentType,
        emptyPublisher) shouldBe HttpEntity.Empty
      PekkoHttpClient.entityForMethodAndContentType(HttpMethods.GET, ContentTypes.NoContentType,
        unknownLengthPublisher, Some(0L)) shouldBe HttpEntity.Empty
    }

    "not drop the request body of a GET or DELETE request when the content length is unknown" in {
      Seq(HttpMethods.GET, HttpMethods.DELETE).foreach { method =>
        val publisher = new SdkHttpContentPublisher {
          override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.empty()
          override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
        }
        val entity =
          PekkoHttpClient.entityForMethodAndContentType(method, ContentTypes.`application/json`, publisher)
        withClue(s"$method: ") {
          entity should not be HttpEntity.Empty
          entity.isKnownEmpty() shouldBe false
          entity.contentLengthOption shouldBe None
          entity.contentType shouldBe ContentTypes.`application/json`
        }
      }
    }

    "never attach a request body for a method that disallows an entity" in {
      val publisher = new SdkHttpContentPublisher {
        override def contentLength(): java.util.Optional[java.lang.Long] = java.util.Optional.of(10L)
        override def subscribe(s: Subscriber[? >: ByteBuffer]): Unit = {}
      }
      PekkoHttpClient.entityForMethodAndContentType(HttpMethods.HEAD, ContentTypes.NoContentType,
        publisher) shouldBe HttpEntity.Empty
    }

    "build() should apply SDK GLOBAL_HTTP_DEFAULTS by default" in {
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .build()
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings.connectionSettings.connectingTimeout shouldBe
      SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toScala
      pekkoClient.connectionSettings.maxConnections shouldBe
      SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.MAX_CONNECTIONS).intValue()
    }

    "buildWithDefaults() should propagate configuration options without explicit opt-in" in {
      val attributeMap = AttributeMap.builder()
        .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, 1.second.toJava)
        .put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, 2.second.toJava)
        .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, Integer.valueOf(3))
        .put(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE, 4.second.toJava)
        .build()
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .buildWithDefaults(attributeMap)
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings.connectionSettings.connectingTimeout shouldBe 1.second
      pekkoClient.connectionSettings.connectionSettings.idleTimeout shouldBe 2.seconds
      pekkoClient.connectionSettings.maxConnections shouldBe 3
      pekkoClient.connectionSettings.maxConnectionLifetime shouldBe 4.seconds
    }

    "withConnectionPoolSettings().buildWithDefaults() should use the passed settings untouched" in {
      val connectionPoolSettings = ConnectionPoolSettings(ConfigFactory.load()).withMaxConnections(7)
      val attributeMap = AttributeMap.builder()
        .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, Integer.valueOf(3))
        .build()
      val pekkoClient: PekkoHttpClient = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .withConnectionPoolSettings(connectionPoolSettings)
        .buildWithDefaults(attributeMap)
        .asInstanceOf[PekkoHttpClient]

      pekkoClient.connectionSettings shouldBe connectionPoolSettings
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

    "give each auto-created client a uniquely named actor system" in {
      val client1 = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .build()
        .asInstanceOf[PekkoHttpClient]
      val client2 = new PekkoHttpAsyncHttpService().createAsyncHttpClientFactory()
        .build()
        .asInstanceOf[PekkoHttpClient]
      try {
        client1.actorSystem.name should startWith("aws-pekko-http")
        client2.actorSystem.name should startWith("aws-pekko-http")
        client1.actorSystem.name should not be client2.actorSystem.name
      } finally {
        client1.close()
        client2.close()
      }
    }

    "not propagate failures from the shutdown handle on close()" in {
      implicit val system: ActorSystem = ActorSystem("pekko-http-client-close-spec")
      try {
        val client = new PekkoHttpClient(
          () => throw new IllegalStateException("simulated shutdown failure"),
          ConnectionPoolSettings(system),
          ConnectionContext.httpsClient(SSLContext.getDefault))(system, system.dispatcher)
        noException should be thrownBy client.close()
      } finally {
        Await.result(system.terminate(), 10.seconds)
      }
    }

    "not propagate an InterruptedException from the shutdown handle on close() and restore the interrupt flag" in {
      implicit val system: ActorSystem = ActorSystem("pekko-http-client-close-interrupt-spec")
      try {
        val client = new PekkoHttpClient(
          () => throw new InterruptedException("simulated interrupt"),
          ConnectionPoolSettings(system),
          ConnectionContext.httpsClient(SSLContext.getDefault))(system, system.dispatcher)
        try {
          noException should be thrownBy client.close()
          Thread.currentThread().isInterrupted shouldBe true
        } finally {
          // clear the flag so it does not leak into the rest of the suite
          Thread.interrupted()
        }
      } finally {
        Await.result(system.terminate(), 10.seconds)
      }
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
