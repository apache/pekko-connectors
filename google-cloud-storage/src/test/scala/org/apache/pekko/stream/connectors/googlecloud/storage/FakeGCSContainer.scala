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

package org.apache.pekko.stream.connectors.googlecloud.storage

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

class FakeGCSContainer(bucketsToCreate: List[String], location: Option[String]) extends GenericContainer(
      "fsouza/fake-gcs-server:1.52",
      exposedPorts = List(4443),
      waitStrategy = Some(Wait.forHttp("/storage/v1/b").forPort(4443).withStartupTimeout(Duration.ofSeconds(10))),
      command = List(
        "-scheme", "http"
      ) ++ location.map(List("-location", _)).getOrElse(Nil)
    ) {
  def getHostAddress: String =
    s"http://${container.getHost}:${container.getMappedPort(4443)}"

  override def start(): Unit = {
    super.start()
    if (bucketsToCreate.nonEmpty) {
      container.execInContainer("mkdir", "data")
      bucketsToCreate.foreach { bucket =>
        container.execInContainer(s"mkdir", s"/data/$bucket")
      }
    }
  }
}
