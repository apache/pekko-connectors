/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.google

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.dispatch.ExecutionContexts
import pekko.http.scaladsl.model.HttpMethods.{ POST, PUT }
import pekko.http.scaladsl.model.StatusCodes.{ Created, OK, PermanentRedirect }
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.headers.ByteRange.Slice
import pekko.http.scaladsl.model.headers.{ `Content-Range`, Location, Range, RawHeader }
import pekko.http.scaladsl.unmarshalling.{ FromResponseUnmarshaller, Unmarshal, Unmarshaller }
import pekko.stream.Materializer
import pekko.stream.connectors.google.http.GoogleHttp
import pekko.stream.connectors.google.util.{ AnnotateLast, EitherFlow, MaybeLast, Retry }
import pekko.stream.scaladsl.{ Flow, Keep, RetryFlow, Sink }
import pekko.util.ByteString

import scala.concurrent.Future
import scala.util.control.NoStackTrace
import scala.util.{ Failure, Success, Try }

@InternalApi
private[connectors] object ResumableUpload {

  final case class InvalidResponseException(override val info: ErrorInfo) extends ExceptionWithErrorInfo(info)
  final case class UploadFailedException() extends Exception
  private final case class Chunk(bytes: ByteString, position: Long)

  /**
   * Initializes and runs a resumable upload to a media endpoint.
   *
   * @see [[https://cloud.google.com/storage/docs/resumable-uploads Cloud Storage documentation]]
   * @see [[https://cloud.google.com/bigquery/docs/reference/api-uploads BigQuery documentation]]
   */
  def apply[T: FromResponseUnmarshaller](request: HttpRequest): Sink[ByteString, Future[T]] = {

    require(request.method == POST, "Resumable upload must be initiated by POST request")
    require(
      request.uri.rawQueryString.exists(_.contains("uploadType=resumable")),
      "Resumable upload must include query parameter `uploadType=resumable`")

    Sink
      .fromMaterializer { (mat, attr) =>
        import mat.executionContext
        implicit val materializer: Materializer = mat
        implicit val settings: GoogleSettings = GoogleAttributes.resolveSettings(mat, attr)
        val uploadChunkSize = settings.requestSettings.uploadChunkSize

        val in = Flow[ByteString]
          .via(chunker(uploadChunkSize))
          .statefulMap(() => 0L)((cumulativeLength, bytes) =>
              (cumulativeLength + bytes.length, Chunk(bytes, cumulativeLength)),
            _ => None)
          .via(AnnotateLast[Chunk])
          .map(chunk => Future.successful(Right(chunk)))

        val upload = Flow
          .lazyFutureFlow { () =>
            initiateSession(request).map { uri =>
              val request = HttpRequest(PUT, uri)
              val flow = Flow[Future[Either[T, MaybeLast[Chunk]]]].mapAsync(1)(identity).via(uploadChunk(request))

              import settings.requestSettings.retrySettings._
              RetryFlow
                .withBackoff(minBackoff, maxBackoff, randomFactor, maxRetries, flow) {
                  case (chunk, Failure(Retry(_))) => Some(updatePosition(request, chunk.map(_.toOption.get)))
                  case _                          => None
                }
                .map(_.recoverWith { case Retry(ex) => Failure(ex) })
            }
          }

        in.via(upload).mapConcat(_.get.toList).toMat(Sink.last)(Keep.right)
      }
      .mapMaterializedValue(_.flatten)
  }

  private def initiateSession(request: HttpRequest)(implicit mat: Materializer,
      settings: GoogleSettings): Future[Uri] = {
    import implicits._

    implicit val um: FromResponseUnmarshaller[Uri] =
      Unmarshaller.withMaterializer { _ => implicit mat => (response: HttpResponse) =>
        if (response.status.isSuccess())
          response.discardEntityBytes().future.map { _ =>
            response.header[Location].fold(throw InvalidResponseException(ErrorInfo("No Location header")))(_.uri)
          }(ExecutionContexts.parasitic)
        else
          Unmarshal(response.entity).to[String].flatMap { errorString =>
            Future.failed(InvalidResponseException(
              ErrorInfo(s"Resumable upload failed with status ${response.status}", errorString)))
          }(ExecutionContexts.parasitic)
      }.withDefaultRetry

    GoogleHttp(mat.system).singleAuthenticatedRequest[Uri](request)
  }

  private final case class DoNotRetry(ex: Throwable) extends Throwable(ex) with NoStackTrace

  private def uploadChunk[T: FromResponseUnmarshaller](
      request: HttpRequest)(implicit mat: Materializer): Flow[Either[T, MaybeLast[Chunk]], Try[Option[T]], NotUsed] = {

    val um = Unmarshaller.withMaterializer { implicit ec => implicit mat => (response: HttpResponse) =>
      response.status match {
        case PermanentRedirect =>
          response.discardEntityBytes().future.map(_ => None)
        case _ =>
          Unmarshal(response).to[T].map(Some(_)).recover { case ex => throw DoNotRetry(ex) }
      }
    }

    val pool = {
      val uri = request.uri
      Flow[HttpRequest]
        .map((_, ()))
        .via(GoogleHttp(mat.system).cachedHostConnectionPoolWithContext(uri.authority.host.address, uri.effectivePort)(
          um))
        .map(_._1.recoverWith { case DoNotRetry(ex) => Failure(ex) })
    }

    EitherFlow(
      Flow[T].map(t => Success(Some(t))),
      Flow[MaybeLast[Chunk]].map {
        case maybeLast @ MaybeLast(Chunk(bytes, position)) =>
          val totalLength = if (maybeLast.isLast) Some(position + bytes.length) else None
          val header = `Content-Range`(ContentRange(position, position + bytes.length - 1, totalLength))
          request.addHeader(header).withEntity(bytes)
      }.via(pool)).map(_.merge).mapMaterializedValue(_ => NotUsed)
  }

  private val statusRequestHeader = RawHeader("Content-Range", "bytes */*")

  private def updatePosition[T: FromResponseUnmarshaller](
      request: HttpRequest,
      chunk: Future[MaybeLast[Chunk]])(
      implicit mat: Materializer, settings: GoogleSettings): Future[Either[T, MaybeLast[Chunk]]] = {
    import implicits._

    implicit val um: FromResponseUnmarshaller[Either[T, Long]] =
      Unmarshaller.withMaterializer { implicit ec => implicit mat => (response: HttpResponse) =>
        response.status match {
          case OK | Created => Unmarshal(response).to[T].map(Left(_))
          case PermanentRedirect =>
            response.discardEntityBytes().future.map { _ =>
              Right(
                response
                  .header[Range]
                  .flatMap(_.ranges.headOption)
                  .collect {
                    case Slice(_, last) => last + 1
                  }.getOrElse(0L))
            }
          case _ => throw InvalidResponseException(ErrorInfo(response.status.value, response.status.defaultMessage))
        }
      }.withDefaultRetry

    import mat.executionContext
    chunk.flatMap {
      case maybeLast @ MaybeLast(Chunk(bytes, position)) =>
        GoogleHttp(mat.system)
          .singleAuthenticatedRequest[Either[T, Long]](request.addHeader(statusRequestHeader))
          .map {
            case Left(result) if maybeLast.isLast => Left(result)
            case Right(newPosition) if newPosition >= position =>
              Right(maybeLast.map { _ =>
                Chunk(bytes.drop(Math.toIntExact(newPosition - position)), newPosition)
              })
            case _ => throw UploadFailedException()
          }
    }
  }

  private def chunker(chunkSize: Int): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString]
      .statefulMap(() => ByteString.newBuilder)((chunkBuilder, bytes) => {
          chunkBuilder ++= bytes
          if (chunkBuilder.length < chunkSize) {
            (chunkBuilder, ByteString.empty)
          } else if (chunkBuilder.length == chunkSize) {
            val chunk = chunkBuilder.result()
            chunkBuilder.clear()
            (chunkBuilder, chunk)
          } else {
            // chunkBuilder.length > chunkSize
            val result = chunkBuilder.result()
            chunkBuilder.clear()
            val (chunk, init) = result.splitAt(chunkSize)
            chunkBuilder ++= init
            (chunkBuilder, chunk)
          }
        }, chunkBuilder => Some(chunkBuilder.result()))
      .filter(_.nonEmpty)
}
