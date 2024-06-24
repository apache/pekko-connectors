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

import java.util.concurrent.CompletableFuture
import java.util.Collections

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.{ ContentTypes, HttpResponse }
import pekko.http.scaladsl.model.headers.{ `Content-Length`, `Content-Type` }
import pekko.stream.Materializer
import pekko.stream.scaladsl.{ Keep, Sink }
import pekko.util.FutureConverters
import org.slf4j.LoggerFactory
import software.amazon.awssdk.http.SdkHttpFullResponse
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler

import scala.concurrent.{ ExecutionContext, Future }

class RequestRunner()(implicit sys: ActorSystem, ec: ExecutionContext, mat: Materializer) {

  val logger = LoggerFactory.getLogger(this.getClass)

  def run(runRequest: () => Future[HttpResponse], handler: SdkAsyncHttpResponseHandler): CompletableFuture[Void] = {
    val result = runRequest().flatMap { response =>
      handler.onHeaders(toSdkHttpFullResponse(response))

      val (complete, publisher) = response.entity.dataBytes
        .filter(_.nonEmpty)
        .map(_.asByteBuffer)
        .alsoToMat(Sink.ignore)(Keep.right)
        .toMat(Sink.asPublisher(fanout = false))(Keep.both)
        .run()

      handler.onStream(publisher)
      complete
    }

    result.failed.foreach(handler.onError)
    FutureConverters.asJava(result.map(_ => null: Void)).toCompletableFuture
  }

  private[awsspi] def toSdkHttpFullResponse(response: HttpResponse): SdkHttpFullResponse =
    SdkHttpFullResponse
      .builder()
      .headers(convertToSdkResponseHeaders(response))
      .statusCode(response.status.intValue())
      .statusText(response.status.reason)
      .build

  private[awsspi] def convertToSdkResponseHeaders(
      response: HttpResponse): java.util.Map[String, java.util.List[String]] = {
    val responseHeaders = new java.util.HashMap[String, java.util.List[String]]

    response.entity.contentType match {
      case ContentTypes.NoContentType => ()
      case contentType                => responseHeaders.put(`Content-Type`.name, Collections.singletonList(contentType.value))
    }

    response.entity.contentLengthOption.foreach(length =>
      responseHeaders.put(`Content-Length`.name, Collections.singletonList(length.toString)))

    response.headers.groupBy(_.name()).foreach { case (k, v) =>
      val values = new java.util.ArrayList[String]
      v.foreach(header => values.add(header.value()))
      responseHeaders.put(k, values)
    }

    responseHeaders
  }
}
