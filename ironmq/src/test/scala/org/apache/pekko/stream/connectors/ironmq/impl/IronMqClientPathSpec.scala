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

package org.apache.pekko.stream.connectors.ironmq.impl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.ironmq.IronMqSettings
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import com.typesafe.config.ConfigFactory

class IronMqClientPathSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem("IronMqClientPathSpec")
  private implicit val mat: Materializer = Materializer.matFromSystem(system)

  private val settings =
    IronMqSettings(ConfigFactory.load().getConfig(IronMqSettings.ConfigPath))
      .withEndpoint(Uri("http://localhost:8080"))
      .withProjectId("proj")
      .withToken("token")

  // no requests are made; only the path construction is exercised
  private val client = new IronMqClient(settings)

  "IronMqClient.queuePath" should {

    "build the expected path for a normal queue name" in {
      client.queuePath("my-queue", "messages").toString shouldBe
      "/3/projects/proj/queues/my-queue/messages"
    }

    "escape a slash so it cannot add path segments" in {
      // without escaping this would become .../queues/a/../../admin/messages
      val path = client.queuePath("a/../../admin", "messages").toString
      path shouldBe "/3/projects/proj/queues/a%2F..%2F..%2Fadmin/messages"
      path should startWith("/3/projects/proj/queues/")
    }

    "escape a question mark so it cannot start a query string" in {
      val uri = Uri(path = client.queuePath("a?x=1", "messages"))
      uri.rawQueryString shouldBe None
      uri.path.toString shouldBe "/3/projects/proj/queues/a%3Fx=1/messages"
    }

    "escape a hash so it cannot start a fragment" in {
      val uri = Uri(path = client.queuePath("a#frag", "messages"))
      uri.fragment shouldBe None
      uri.path.toString shouldBe "/3/projects/proj/queues/a%23frag/messages"
    }

    "accept a queue name with a space" in {
      // Uri(s"...$name...") would throw IllegalUriException for this name
      client.queuePath("a b", "messages").toString shouldBe
      "/3/projects/proj/queues/a%20b/messages"
    }

    "keep every queue name under the queues path" in {
      val hostile =
        Seq("a/../../admin", "a?x=1", "a#frag", "../../other", "a b", "..", "/etc/passwd")
      hostile.foreach { name =>
        withClue(s"queue name '$name': ") {
          client.queuePath(name, "messages").toString should startWith("/3/projects/proj/queues/")
        }
      }
    }
  }

  override def afterAll(): Unit = {
    system.terminate()
    ()
  }
}
