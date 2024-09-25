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
import pekko.http.scaladsl._
import pekko.http.scaladsl.model.HttpHeader.ParsingResult
import pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import pekko.http.scaladsl.model.MediaType.Compressible
import pekko.http.scaladsl.model.RequestEntityAcceptance.Expected
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.headers.{ `Content-Length`, `Content-Type` }
import pekko.http.scaladsl.settings.ConnectionPoolSettings
import pekko.stream.scaladsl._
import pekko.stream.{ Materializer, OverflowStrategy, SystemMaterializer }
import pekko.util.ByteString
import pekko.util.OptionConverters._
import pekko.util.JavaDurationConverters._
import org.slf4j.LoggerFactory
import software.amazon.awssdk.http.async._
import software.amazon.awssdk.http.{ Protocol, SdkHttpConfigurationOption, SdkHttpRequest }
import software.amazon.awssdk.utils.AttributeMap

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._
import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future, Promise }

class PekkoHttpClient(
    shutdownHandle: () => Unit,
    protocol: HttpProtocol,
    private[awsspi] val connectionSettings: ConnectionPoolSettings,
    private[awsspi] val connectionContext: HttpsConnectionContext
)(
    implicit
    actorSystem: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer) extends SdkAsyncHttpClient {
  import PekkoHttpClient._

  private lazy val runner = new RequestRunner()
  private lazy val http2connectionFlows =
    new java.util.concurrent.ConcurrentHashMap[Uri, SourceQueueWithComplete[HttpRequest]]()

  override def execute(request: AsyncExecuteRequest): CompletableFuture[Void] = {

    logger.debug(s"Executing with protocol: $protocol")

    if (protocol == HttpProtocols.`HTTP/2.0`) {
      val useTls = request.request().protocol() == "https"
      val akkaHttpRequest = toPekkoRequest(/*protocol, */ request.request(), request.requestContentPublisher())
      val uri = akkaHttpRequest.effectiveUri(securedConnection = useTls)
      val queue = http2connectionFlows.computeIfAbsent(uri,
        _ => {
          val baseConnection = Http()
            .connectionTo(request.request().host())
            .toPort(request.request().port())
            .withCustomHttpsConnectionContext(connectionContext)
          val http2client = request.request().protocol() match {
            case "http"  => baseConnection.managedPersistentHttp2WithPriorKnowledge()
            case "https" => baseConnection.managedPersistentHttp2()
            case _       => throw new IllegalArgumentException("Unsupported protocol")
          }
          Source
            .queue[HttpRequest](4242, OverflowStrategy.fail)
            .via(http2client)
            .to(Sink.foreach { res =>
              res.attribute(ResponsePromise.Key).get.promise.trySuccess(res)
            })
            .run()
        })

      val dispatch: HttpRequest => Future[HttpResponse] = req => {
        val p = Promise[HttpResponse]()
        queue.offer(req.addAttribute(ResponsePromise.Key, ResponsePromise(p))).flatMap(_ => p.future)
      }

      runner.run(
        () => dispatch(akkaHttpRequest),
        request.responseHandler()
      )
    } else {
      runner.run(
        () => {
          val pekkoHttpRequest = toPekkoRequest(request.request(), request.requestContentPublisher())
          Http().singleRequest(pekkoHttpRequest, settings = connectionSettings, connectionContext = connectionContext)
        },
        request.responseHandler())
    }
  }

  override def close(): Unit =
    shutdownHandle()

  override def clientName(): String = "pekko-http"
}

object PekkoHttpClient {

  private val logger = LoggerFactory.getLogger(this.getClass)

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

      println("serviceDefaults: " + serviceDefaults)

      val resolvedOptions = serviceDefaults.merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);

      val protocol = toProtocol(resolvedOptions.get(SdkHttpConfigurationOption.PROTOCOL))

      val cps = connectionPoolSettingsBuilder(
        connectionPoolSettings.getOrElse(ConnectionPoolSettings(as)),
        resolvedOptions
      )

      val connectionContext =
        if (resolvedOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES).booleanValue())
          ConnectionContext.httpsClient(createInsecureSslEngine _)
        else ConnectionContext.httpsClient(SSLContext.getDefault)

      val shutdownhandleF = () => {
        if (actorSystem.isEmpty) {
          Await.result(Http().shutdownAllConnectionPools().flatMap(_ => as.terminate()),
            Duration.apply(10, TimeUnit.SECONDS))
        }
        ()
      }
      new PekkoHttpClient(shutdownhandleF, protocol, cps, connectionContext)(as, ec, mat)
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

  private def toProtocol(protocol: Protocol): HttpProtocol = protocol match {
    case Protocol.HTTP2   => HttpProtocols.`HTTP/2.0`
    case Protocol.HTTP1_1 => HttpProtocols.`HTTP/1.1`
    case _                => throw new IllegalArgumentException(s"Unsupported protocol: $protocol")
  }

  private def createInsecureSslEngine(host: String, port: Int): SSLEngine = {
    val engine = createTrustfulSslContext().createSSLEngine(host, port)
    engine.setUseClientMode(true)

    // WARNING: this creates an SSL Engine without enabling endpoint identification/verification procedures
    // Disabling host name verification is a very bad idea, please don't unless you have a very good reason to.
    // When in doubt, use the `ConnectionContext.httpsClient` that takes an `SSLContext` instead, or enable with:
    // engine.setSSLParameters({
    //   val params = engine.getSSLParameters
    //   params.setEndpointIdentificationAlgorithm("https")
    //   params
    // })

    engine
  }

  private def createTrustfulSslContext(): SSLContext = {
    object NoCheckX509TrustManager extends X509TrustManager {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()

      override def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()

      override def getAcceptedIssuers = Array[X509Certificate]()
    }

    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), new SecureRandom())
    context
  }
}
