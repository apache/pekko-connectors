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

package org.apache.pekko.stream.connectors.mqtt.streaming

import java.util.Optional
import java.util.concurrent.{ CompletableFuture, CompletionStage, ExecutionException, TimeUnit }

import org.apache.pekko
import pekko.Done
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CommandSpec extends AnyWordSpec with Matchers {

  private val packet = PubAck(PacketId(1))

  "Command Java API" should {

    "complete the CompletableFuture when the session completes the command" in {
      val completed = new CompletableFuture[Done]()
      val command = new Command[String](packet, Optional.of[CompletionStage[Done]](completed), Optional.empty[String]())

      command.completed.get.success(Done)

      completed.get(3, TimeUnit.SECONDS) shouldBe Done
    }

    "complete the CompletableFuture exceptionally when the session fails the command" in {
      val completed = new CompletableFuture[Done]()
      val command = new Command[String](packet, Optional.of[CompletionStage[Done]](completed), Optional.empty[String]())
      val failure = new RuntimeException("cannot route")

      command.completed.get.failure(failure)

      val thrown = the[ExecutionException] thrownBy completed.get(3, TimeUnit.SECONDS)
      thrown.getCause shouldBe theSameInstanceAs(failure)
    }

    "reject a CompletionStage that is not a CompletableFuture" in {
      val stage: CompletionStage[Done] = new CompletableFuture[Done]().minimalCompletionStage()

      an[IllegalArgumentException] should be thrownBy
      new Command[String](packet, Optional.of(stage), Optional.empty[String]())
    }

    "carry data without a completion" in {
      val command = new Command[String](packet, Optional.empty[CompletionStage[Done]](), Optional.of("carry"))

      command.completed shouldBe None
      command.carry shouldBe Some("carry")
    }
  }
}
