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

import java.util.concurrent.{ CompletableFuture, TimeUnit }

import org.apache.pekko
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model.HttpHeader.ParsingResult
import pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import pekko.http.scaladsl.model.MediaType.Compressible
import pekko.http.scaladsl.model.RequestEntityAcceptance.Expected
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.headers.{ `Content-Length`, `Content-Type` }
import pekko.http.scaladsl.settings.ConnectionPoolSettings
import pekko.stream.scaladsl.Source
import pekko.stream.{ Materializer, SystemMaterializer }
import pekko.util.ByteString
import pekko.util.OptionConverters._
import pekko.util.JavaDurationConverters._
import org.slf4j.LoggerFactory
import software.amazon.awssdk.http.async._
import software.amazon.awssdk.http.{ SdkHttpConfigurationOption, SdkHttpRequest }
import software.amazon.awssdk.utils.AttributeMap

import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class PekkoHttpClient(shutdownHandle: () => Unit, private[awsspi] val connectionSettings: ConnectionPoolSettings)(
    implicit
    actorSystem: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer) extends SdkAsyncHttpClient {
  import PekkoHttpClient._

  lazy val runner = new RequestRunner()

  override def execute(request: AsyncExecuteRequest): CompletableFuture[Void] = {
    val pekkoHttpRequest = toPekkoRequest(request.request(), request.requestContentPublisher())
    runner.run(
      () => Http().singleRequest(pekkoHttpRequest, settings = connectionSettings),
      request.responseHandler())
  }

  override def close(): Unit =
    shutdownHandle()

  override def clientName(): String = "pekko-http"
}

object PekkoHttpClient {

  val logger = LoggerFactory.getLogger(this.getClass)

  private[awsspi] def toPekkoRequest(request: SdkHttpRequest,
      contentPublisher: SdkHttpContentPublisher): HttpRequest = {
    val (contentTypeHeader, reqheaders) = convertHeaders(request.headers())
    val method = convertMethod(request.method().name())
    HttpRequest(
      method = method,
      uri = Uri(request.getUri.toString),
      headers = reqheaders,
      entity =
        entityForMethodAndContentType(method, contentTypeHeaderToContentType(contentTypeHeader), contentPublisher),
      protocol = HttpProtocols.`HTTP/1.1`)
  }

  private[awsspi] def entityForMethodAndContentType(method: HttpMethod,
      contentType: ContentType,
      contentPublisher: SdkHttpContentPublisher): RequestEntity =
    method.requestEntityAcceptance match {
      case Expected => contentPublisher.contentLength().toScala match {
          case Some(length) =>
            HttpEntity(contentType, length, Source.fromPublisher(contentPublisher).map(ByteString(_)))
          case None => HttpEntity(contentType, Source.fromPublisher(contentPublisher).map(ByteString(_)))
        }
      case _ => HttpEntity.Empty
    }

  private[awsspi] def convertMethod(method: String): HttpMethod =
    HttpMethods
      .getForKeyCaseInsensitive(method)
      .getOrElse(throw new IllegalArgumentException(s"Method not configured: $method"))

  private[awsspi] def contentTypeHeaderToContentType(contentTypeHeader: Option[HttpHeader]): ContentType =
    contentTypeHeader
      .map(_.value())
      .map(v => contentTypeMap.getOrElse(v, tryCreateCustomContentType(v)))
      // Its allowed to not have a content-type: https://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.2.1
      //
      //  Any HTTP/1.1 message containing an entity-body SHOULD include a Content-Type header field defining the media type
      //  of that body. If and only if the media type is not given by a Content-Type field, the recipient MAY attempt to
      //  guess the media type via inspection of its content and/or the name extension(s) of the URI used to identify the
      //  resource. If the media type remains unknown, the recipient SHOULD treat it as type "application/octet-stream".
      //
      .getOrElse(ContentTypes.NoContentType)

  // This method converts the headers to Akka-http headers and drops content-length and returns content-type separately
  private[awsspi] def convertHeaders(
      headers: java.util.Map[String, java.util.List[String]]): (Option[HttpHeader], immutable.Seq[HttpHeader]) = {
    val headersAsScala = {
      val builder = collection.mutable.Map.newBuilder[String, java.util.List[String]]
      headers.forEach { case (k, v) => builder += k -> v }
      builder.result()
    }

    headersAsScala.foldLeft((Option.empty[HttpHeader], List.empty[HttpHeader])) { case ((ctHeader, hdrs), header) =>
      val (headerName, headerValue) = header
      if (headerValue.size() != 1) {
        throw new IllegalArgumentException(
          s"Found invalid header: key: $headerName, Value: ${val list = List.newBuilder[String]
            headerValue.forEach(v => list += v)
            list.result()}.")
      }
      // skip content-length as it will be calculated by pekko-http itself and must not be provided in the request headers
      if (`Content-Length`.lowercaseName == headerName.toLowerCase) (ctHeader, hdrs)
      else {
        HttpHeader.parse(headerName, headerValue.get(0)) match {
          case ok: Ok =>
            // return content-type separately as it will be used to calculate ContentType, which is used on HttpEntity
            if (ok.header.lowercaseName() == `Content-Type`.lowercaseName) (Some(ok.header), hdrs)
            else (ctHeader, hdrs :+ ok.header)
          case error: ParsingResult.Error =>
            throw new IllegalArgumentException(s"Found invalid header: ${error.errors}.")
        }
      }
    }
  }

  private[awsspi] def tryCreateCustomContentType(contentTypeStr: String): ContentType = {
    logger.debug(s"Try to parse content type from $contentTypeStr")
    val mainAndsubType = contentTypeStr.split('/')
    if (mainAndsubType.length == 2)
      ContentType(MediaType.customBinary(mainAndsubType(0), mainAndsubType(1), Compressible))
    else throw new RuntimeException(s"Could not parse custom content type '$contentTypeStr'.")
  }

  private[awsspi] def buildConnectionPoolSettings(
      base: ConnectionPoolSettings, attributeMap: AttributeMap): ConnectionPoolSettings = {
    def zeroToInfinite(duration: java.time.Duration): scala.concurrent.duration.Duration =
      if (duration.isZero) scala.concurrent.duration.Duration.Inf
      else duration.asScala

    base
      .withUpdatedConnectionSettings(s =>
        s.withConnectingTimeout(attributeMap.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).asScala)
          .withIdleTimeout(attributeMap.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).asScala))
      .withMaxConnections(attributeMap.get(SdkHttpConfigurationOption.MAX_CONNECTIONS).intValue())
      .withMaxConnectionLifetime(zeroToInfinite(attributeMap.get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE)))
  }

  def builder() = PekkoHttpClientBuilder()

  case class PekkoHttpClientBuilder(private val actorSystem: Option[ActorSystem] = None,
      private val executionContext: Option[ExecutionContext] = None,
      private val connectionPoolSettings: Option[ConnectionPoolSettings] = None,
      private val connectionPoolSettingsBuilder: (ConnectionPoolSettings, AttributeMap) => ConnectionPoolSettings =
        (c, _) => c)
      extends SdkAsyncHttpClient.Builder[PekkoHttpClientBuilder] {
    def buildWithDefaults(serviceDefaults: AttributeMap): SdkAsyncHttpClient = {
      implicit val as = actorSystem.getOrElse(ActorSystem("aws-pekko-http"))
      implicit val ec = executionContext.getOrElse(as.dispatcher)
      val mat: Materializer = SystemMaterializer(as).materializer

      val resolvedOptions = serviceDefaults.merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);

      val cps = connectionPoolSettingsBuilder(
        connectionPoolSettings.getOrElse(ConnectionPoolSettings(as)),
        resolvedOptions
      )
      val shutdownhandleF = () => {
        if (actorSystem.isEmpty) {
          Await.result(Http().shutdownAllConnectionPools().flatMap(_ => as.terminate()),
            Duration.apply(10, TimeUnit.SECONDS))
        }
        ()
      }
      new PekkoHttpClient(shutdownhandleF, cps)(as, ec, mat)
    }
    def withActorSystem(actorSystem: ActorSystem): PekkoHttpClientBuilder = copy(actorSystem = Some(actorSystem))
    def withActorSystem(actorSystem: ClassicActorSystemProvider): PekkoHttpClientBuilder =
      copy(actorSystem = Some(actorSystem.classicSystem))
    def withExecutionContext(executionContext: ExecutionContext): PekkoHttpClientBuilder =
      copy(executionContext = Some(executionContext))
    def withConnectionPoolSettings(connectionPoolSettings: ConnectionPoolSettings): PekkoHttpClientBuilder =
      copy(connectionPoolSettings = Some(connectionPoolSettings))
    def withConnectionPoolSettingsBuilder(
        connectionPoolSettingsBuilder: (ConnectionPoolSettings, AttributeMap) => ConnectionPoolSettings
    ): PekkoHttpClientBuilder =
      copy(connectionPoolSettingsBuilder = connectionPoolSettingsBuilder)
    def withConnectionPoolSettingsBuilderFromAttributeMap(): PekkoHttpClientBuilder =
      copy(connectionPoolSettingsBuilder = buildConnectionPoolSettings)
  }

  lazy val xAmzJson = ContentType(MediaType.customBinary("application", "x-amz-json-1.0", Compressible))
  lazy val xAmzJson11 = ContentType(MediaType.customBinary("application", "x-amz-json-1.1", Compressible))
  lazy val xAmzCbor11 = ContentType(MediaType.customBinary("application", "x-amz-cbor-1.1", Compressible))
  lazy val formUrlEncoded =
    ContentType(MediaType.applicationWithOpenCharset("x-www-form-urlencoded"), HttpCharset.custom("utf-8"))
  lazy val applicationXml = ContentType(MediaType.customBinary("application", "xml", Compressible))

  lazy val contentTypeMap: collection.immutable.Map[String, ContentType] = collection.immutable.Map(
    "application/x-amz-json-1.0" -> xAmzJson,
    "application/x-amz-json-1.1" -> xAmzJson11,
    "application/x-amz-cbor-1.1" -> xAmzCbor11, // used by Kinesis
    "application/x-www-form-urlencoded; charset-UTF-8" -> formUrlEncoded,
    "application/x-www-form-urlencoded" -> formUrlEncoded,
    "application/xml" -> applicationXml)
}
