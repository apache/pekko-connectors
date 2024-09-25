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

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.headers.`User-Agent`
import pekko.http.scaladsl.model.{ HttpEntity, HttpResponse }
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler

import scala.concurrent.Future

class RequestRunnerSpec extends AnyWordSpec with Matchers with OptionValues {
  "Check headers are present from response" in {
    implicit val system = ActorSystem("test")
    implicit val ec = system.dispatcher
    val response = HttpResponse(entity = HttpEntity("Ok"), headers = `User-Agent`("Mozilla") :: Nil)
    val runner = new RequestRunner()
    val handler = new MyHeaderHandler()
    val resp = runner.run(() => Future.successful(response), handler)
    resp.join()

    handler.responseHeaders.headers().get("User-Agent").get(0) shouldBe "Mozilla"
    handler.responseHeaders.headers().get("Content-Type").get(0) shouldBe "text/plain; charset=UTF-8"
    handler.responseHeaders.headers().get("Content-Length").get(0) shouldBe "2"
  }

  private class MyHeaderHandler() extends SdkAsyncHttpResponseHandler {
    private val headers = new AtomicReference[SdkHttpResponse](null)
    def responseHeaders = headers.get()
    override def onHeaders(headers: SdkHttpResponse): Unit = this.headers.set(headers)
    override def onStream(stream: Publisher[ByteBuffer]): Unit = stream.subscribe(new MySubscriber)
    override def onError(error: Throwable): Unit = ()
  }

  private class MySubscriber() extends Subscriber[ByteBuffer] {
    override def onSubscribe(s: Subscription): Unit = s.request(1000)
    override def onNext(t: ByteBuffer): Unit = ()
    override def onError(t: Throwable): Unit = ()
    override def onComplete(): Unit = ()
  }
}
